/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.activity;


import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.springframework.web.client.ResourceAccessException;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import at.univie.compass.R;
import at.univie.compass.bluetooth.BLEDeviceScanActivity;
import at.univie.compass.bluetooth.BLETestHRM;
import at.univie.compass.dto.SharedCourse;
import at.univie.compass.global.Global;
import at.univie.compass.task.CourseFetchTask;
import at.univie.compass.task.StartupAuthTask;
import at.univie.compass.task.TileDownloadTask;
import at.univie.compass.task.UploadResultWorker;


public class MainActivity extends AppCompatActivity implements AuthCallbackInterface {
    /* Initial activity with access to scanning, running and various options. */

    // UI
    TextView courseTextView, welcomeTextView, versionTextView, loadingMapTextView;
    ProgressBar progressBar;
    Snackbar snack;
    MaterialCardView runCardButton, scanCardButton, firstVisitCardButton;
    CoordinatorLayout coordinatorLayout;
    private IntentIntegrator qrScan;

    // Data
    SharedCourse selected_course = null;
    SharedCourse downloadedCourse = null;
    String scannedQRCode = "";

    // Tasks
    TileDownloadTask tdt;
    CourseFetchTask rt;

    // State
    boolean scanning = false;


    @Override
    protected void onDestroy() {
        if (tdt != null) {
            tdt.cancel(true);
        }
        if (rt != null) {
            rt.cancel(true);
        }

        // otherwise the app seem to quit sometimes (cf. open-and-close-QR-scan in RunActivity)
        if (!scanning) {
            finishAndRemoveTask();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_nickname:
                openNicknameDialog();
                return true;
            case R.id.show_results:
                openResultListActivity();
                return true;
            case R.id.delete_storage:
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
                builder.setMessage(getString(R.string.do_you_want_to_delete))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                // "Yes" button was clicked
                                Global.deleteAllStoredResults(getApplicationContext());
                                Global.deleteAllStoredViewCodes(getApplicationContext());
                                setSelectedCourse(null);  // also deletes SC + overlays
                                WorkManager.getInstance().cancelAllWorkByTag(Global.UPLOAD_WORK_ID);
                                WorkManager.getInstance().cancelAllWorkByTag(Global.PROCESS_WORK);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            case R.id.manage_courses:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse((Global.USE_HTTPS ? "https" : "http") + "://" + Global.DEFAULT_BACKEND_HOST + ":" + Global.WEBSITE_PORT + Global.WEBSITE_MANAGE_COURSES_FILE));
                startActivity(browserIntent);
                return true;
            case R.id.connect_ble:
                Intent bleScanIntent = new Intent(MainActivity.this, BLEDeviceScanActivity.class);
                startActivity(bleScanIntent);
                return true;
            case R.id.test_ble:
                Intent hrmTestIntent = new Intent(MainActivity.this, BLETestHRM.class);
                startActivity(hrmTestIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Global.clearValuesForSurvival(getApplicationContext());

        versionTextView = findViewById(R.id.versionTextview);
        versionTextView.setText(getString(R.string.version, Global.getVersionName()));

        StartupAuthTask sat = new StartupAuthTask(this, true, false);
        sat.execute();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // UI
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        courseTextView = findViewById(R.id.courseTextView);
        welcomeTextView = findViewById(R.id.welcomeTextview);

        runCardButton = findViewById(R.id.run_card);
        scanCardButton = findViewById(R.id.scan_card);
        firstVisitCardButton = findViewById(R.id.first_visit_card);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setProgress(0);
        loadingMapTextView = findViewById(R.id.loadingMapTextView);
        loadingMapTextView.setVisibility(View.INVISIBLE);

        // Retrieve stored course + nickname + backendAddress
        SharedCourse storedCourse = Global.getStoredCourse(getApplicationContext());
        setSelectedCourse(storedCourse);

        String nickname = Global.getStoredNickname(getApplicationContext());
        if (nickname.equals("")) {
            openNicknameDialog();
        }
        else {
            welcomeTextView.setText(getString(R.string.welcome_name, Global.getStoredNickname(this)));
        }

        // To invoke barcode scanning
        qrScan = new IntentIntegrator(this).setBeepEnabled(false);

        runCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean sessionOk = checkSessionTimeRange(selected_course);
                if (sessionOk) {
                    openRunActivity(selected_course, Global.getStoredNickname(MainActivity.this), false);
                }
            }
        });
        scanCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanning = true;
                qrScan.initiateScan();
            }
        });
        firstVisitCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse((Global.USE_HTTPS ? "https" : "http") + "://" + Global.DEFAULT_BACKEND_HOST + ":" + Global.WEBSITE_PORT));
                startActivity(browserIntent);
            }
        });


        // Try to send results on app startup
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadResultWorker.class)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(Global.UPLOAD_WORK_ID, ExistingWorkPolicy.REPLACE, uploadWorkRequest);  // https://developer.android.com/topic/libraries/architecture/workmanager/advanced#unique
    }

    public void openResultListActivity() {
        Intent intent = new Intent(this, ResultListActivity.class);
        startActivity(intent);
    }

    public void updateProgressBar(int value) {
        progressBar.setProgress(value);
    }

    // Process the scan results, see https://zxing.github.io/zxing/apidocs/com/google/zxing/integration/android/IntentIntegrator.html
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case IntentIntegrator.REQUEST_CODE:
                scanning = false;
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (result != null && result.getContents() != null) {
                    String qrContent = result.getContents();
                    scannedQRCode = qrContent;

                    StartupAuthTask sat = new StartupAuthTask(this, false, true);
                    sat.execute();

                    Global.snack(this, getString(R.string.please_wait_while_the_course_is_loaded), Snackbar.LENGTH_LONG);
                } else {
                    Global.snack(this, getString(R.string.no_course_qr_code_scanned), Snackbar.LENGTH_LONG);
                }
        }
    }

    public void openRunActivity(SharedCourse courseToRun, String nickname, boolean dummy) {
        // dummy: do not show RunActivity. Used to get required zoom level of WebView.
        Intent intent = new Intent(this, RunActivity.class);
        RunActivity.currentSharedCourse = courseToRun;
        intent.putExtra("nickname", nickname);
        intent.putExtra("dummy", dummy);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        if (dummy) {
            RunActivity.callerMainActivity = new WeakReference<>(this);;
        } else {
            RunActivity.callerMainActivity = new WeakReference<>(this);;
        }
        startActivity(intent);
    }

    public void startTileDownload(int minZoomLevel, int maxZoomLevel, double minx, double miny, double maxx, double maxy)  // openlayers extent, see https://openlayers.org/en/latest/apidoc/module-ol_extent.html
    {
        tdt = new TileDownloadTask(this, minZoomLevel, maxZoomLevel, minx, miny, maxx, maxy, Global.TILESERVER_URLS);// , selected_course.getCourse());
        tdt.execute();
    }

    private void openNicknameDialog() {
        // https://stackoverflow.com/questions/40261250/validation-on-edittext-in-alertdialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setMessage(R.string.enter_nickname);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(Global.getStoredNickname(this));
        input.setSelection(input.getText().length());
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(20)});

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.alert_edittext_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.alert_edittext_margin);
        input.setLayoutParams(params);
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // empty because onClick is overridden below to avoid closing the dialog by default after onClick()
            }
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = input.getText().toString().trim();
                if (name.equals("")) {
                    snack = Snackbar.make(coordinatorLayout, getString(R.string.name_cannot_be_empty), Snackbar.LENGTH_INDEFINITE);
                    View view = snack.getView();
                    CoordinatorLayout.LayoutParams params =(CoordinatorLayout.LayoutParams)view.getLayoutParams();
                    params.gravity = Gravity.TOP;
                    view.setLayoutParams(params);
                    snack.show();
                } else {
                    if(snack != null)
                        snack.dismiss();
                    Global.storeNickname(getApplicationContext(), name);
                    welcomeTextView.setText(getString(R.string.welcome_name, Global.getStoredNickname(MainActivity.this)));
                    alertDialog.dismiss();
                }
            }
        });
    }

    public void setSelectedCourse(SharedCourse course) {
        selected_course = course;
        if (course != null) {
            Global.storeCourse(getApplicationContext(), selected_course);
            courseTextView.setText(String.format("%s (%s)", selected_course.getCourse().getName(), selected_course.getGameModus().name()));
            runCardButton.setVisibility(View.VISIBLE);
            firstVisitCardButton.setVisibility(View.GONE);
        } else {
            Global.deleteStoredCourse(getApplicationContext());
            courseTextView.setText("");
            runCardButton.setVisibility(View.GONE);
            firstVisitCardButton.setVisibility(View.VISIBLE);
        }
    }

    public void onTileDownloadTaskFinished(boolean success, boolean missingTilesDueToInternet) {
        progressBar.setVisibility(View.INVISIBLE);
        loadingMapTextView.setVisibility(View.INVISIBLE);
        scanCardButton.setEnabled(true);
        runCardButton.setEnabled(true);

        if (missingTilesDueToInternet) {
            Global.snack(this, getString(R.string.missing_tiles_due_to_internet), Snackbar.LENGTH_LONG);
        } else {
            Global.snack(this, getString(R.string.course_now_available), Snackbar.LENGTH_LONG);
            setSelectedCourse(downloadedCourse);
        }
    }

    private boolean checkSessionTimeRange(SharedCourse sharedCourse) {
        Date now = Global.now();
        Date start = null;
        Date end = null;
        try {
            start = Global.stringToDateNoMillis(sharedCourse.getTimeStampStart());
            end = Global.stringToDateNoMillis(sharedCourse.getTimeStampEnd());
        } catch (ParseException | NullPointerException ignored) {
        }

        if (start != null && now.before(start) || end != null && now.after(end)) {
            // outside event range -> error message
            String msg = "";
            final DateFormat MSG_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            if (now.before(start)) {
                msg = getString(R.string.session_has_not_startet_yet, MSG_DATETIME_FORMAT.format(start));
            } else if (now.after(end)) {
                msg = getString(R.string.session_has_already_ended, MSG_DATETIME_FORMAT.format(end));
            }
            Global.snack(this, msg, Snackbar.LENGTH_INDEFINITE);
            return false;
        }
        return true;
    }

    @Override
    public void onAuthTaskFinished(String result, boolean alsoRecommend, boolean fromScan) {
        Global.authFailedDialog(this, result, alsoRecommend);
        if (fromScan && !result.equals(Global.LOGIN_UPDATE_REQUIRED)) {
            scanCardButton.setEnabled(false);  // disable buttons while loading course
            runCardButton.setEnabled(false);
            rt = new CourseFetchTask(this);
            rt.execute(scannedQRCode);
        }
    }

    public void onCourseFetchTaskFinished(final SharedCourse course, Exception error, Integer specialErrorMsgStringId, String specialErrorMsgStringArg) {  // gets called from within CourseFetchTask
        if (course != null) {
            // within event range or no range exists -> download course
            Global.snack(this, getString(R.string.caching_the_map), Snackbar.LENGTH_LONG);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            loadingMapTextView.setVisibility(View.VISIBLE);
            downloadedCourse = course;
            downloadedCourse.viaOneTimeCode = !scannedQRCode.equals(downloadedCourse.getQrCode());

            // load WebView (in RunActivity) in dummy mode to get the zoom level required to fetch tiles
            openRunActivity(course, Global.getStoredNickname(getApplicationContext()), true);
        } else {
            String em = null;
            if (specialErrorMsgStringId != null) {
                if (specialErrorMsgStringArg != null) {
                    em = getString(specialErrorMsgStringId, specialErrorMsgStringArg);
                } else {
                    em = getString(specialErrorMsgStringId);
                }
            } else if (error != null) {
                if (error instanceof ResourceAccessException) {
                    em = getString(R.string.error_cannot_fetch_course_probably_due_to_internet);
                } else {
                    em = error.getLocalizedMessage();
                }
            }

            snack = Snackbar.make(coordinatorLayout, em, Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    });
            View view = snack.getView();
            TextView textView = (TextView) view.findViewById(com.google.android.material.R.id.snackbar_text);
            textView.setMaxLines(10);  // show multiple line
            CoordinatorLayout.LayoutParams params =(CoordinatorLayout.LayoutParams)view.getLayoutParams();
            params.gravity = Gravity.TOP;
            view.setLayoutParams(params);
            snack.show();

            scanCardButton.setEnabled(true);
            runCardButton.setEnabled(true);
        }
    }
}
