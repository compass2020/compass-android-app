/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;

import at.univie.compass.global.Global;
import at.univie.compass.R;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BLEDeviceScanActivity extends AppCompatActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private String mDeviceName;
    Snackbar snack;
    private CoordinatorLayout coordinatorLayout;
    private MaterialTextView bleStatusText;
    private String mDeviceAddress;
    private Handler mHandler;
    private ListView listView;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CODE_ENABLE_GPS_WINDOW = 4823;  // arbitrary
    private static final int TEST_BLE_HRM = 4711;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private static int PERMISSION_REQUEST_CODE = 1;
    private final static String TAG = BLEDeviceScanActivity.class.getSimpleName();

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        if(requestCode == PERMISSION_REQUEST_CODE)
        {
            //Do something based on grantResults
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Log.d(TAG, "coarse location permission granted");
            }
            else
            {
                Log.d(TAG, "coarse location permission denied");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_ble);

        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        bleStatusText = findViewById(R.id.bleStatus);

        Log.d(TAG, "Request Location Permissions:");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        }

        super.onCreate(savedInstanceState);

        listView = findViewById(R.id.ble_listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;

                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }

                mDeviceAddress = device.getAddress();
                mDeviceName = device.getName();

                Global.storeBLEaddress(BLEDeviceScanActivity.this, mDeviceAddress);
                Global.storeBLEname(BLEDeviceScanActivity.this, mDeviceName);

                bleStatusText.setText("");
                Intent hrmTestIntent = new Intent(BLEDeviceScanActivity.this, BLECheckConnection.class);
                startActivityForResult(hrmTestIntent, TEST_BLE_HRM);

        /*final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());*/

                // startActivity(intent);
            }
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check if location is enabled
        if (!isLocationEnabled()) {
            final AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.turn_on_location))
                    .setPositiveButton(R.string.location_settings, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_CODE_ENABLE_GPS_WINDOW);
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                        }
                    });
            builder.create().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ble_scan_menu, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);

        scanLeDevice(true);
    }

    boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        if(requestCode == TEST_BLE_HRM) {
            if(resultCode == RESULT_OK) {
                snack = Snackbar.make(coordinatorLayout, getString(R.string.connection_successful), Snackbar.LENGTH_LONG);
                View view = snack.getView();
                CoordinatorLayout.LayoutParams params =(CoordinatorLayout.LayoutParams)view.getLayoutParams();
                params.gravity = Gravity.TOP;
                view.setLayoutParams(params);
                bleStatusText.setText(getString(R.string.connected_please_make_sure, Global.getStoredBLEname(BLEDeviceScanActivity.this)));
            }else/* if(resultCode == RESULT_FIRST_USER) {
                snack = Snackbar.make(coordinatorLayout, "Connection terminated!", Snackbar.LENGTH_LONG);
                View view = snack.getView();
                CoordinatorLayout.LayoutParams params =(CoordinatorLayout.LayoutParams)view.getLayoutParams();
                params.gravity = Gravity.TOP;
                view.setLayoutParams(params);
                bleStatusText.setText("Connection was terminated. Please make sure that you wear the device, otherwise it cannot be connected.");
            }
            else*/
            {
                Global.storeBLEaddress(BLEDeviceScanActivity.this,"");
                Global.storeBLEname(BLEDeviceScanActivity.this,"");
                snack = Snackbar.make(coordinatorLayout, getString(R.string.connection_failed), Snackbar.LENGTH_LONG);
                View view = snack.getView();
                CoordinatorLayout.LayoutParams params =(CoordinatorLayout.LayoutParams)view.getLayoutParams();
                params.gravity = Gravity.TOP;
                view.setLayoutParams(params);
                bleStatusText.setText(getString(R.string.connection_unsuccessful));
            }
            snack.show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = BLEDeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
