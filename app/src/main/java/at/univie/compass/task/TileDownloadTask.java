/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.task;

// TODO: Problem: Zoom 12,6 lädt 13er-tiles und 12,4 wohl 12er-tiles. Ist das sicher genug, um bei 12,6 keine 12er-tiles zu laden? (im Moment konservativ)
// TODO: Muss auch funktionieren, wenn Tile-Server nicht verfügbar, IP gesperrt etc.
// TODO: HTTP 503 Service Unavailable immer noch tlw beim Tile download (10 threads, wenn man direkt hintereinander nochmal scannt) -> vllt einfach bei 10 lassen und nix doppelt cachen?? vllt erkennt das der server wenn man doppelte anfragen schickt.

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import at.univie.compass.global.Global;
import at.univie.compass.activity.MainActivity;


class Tile {
    int x, y, z;
    Tile(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "Tile{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}

class SingleTileDownloadTask implements Callable<Boolean> {
    private URL tileURL;
    private WeakReference<MainActivity> callerMainActivity;
    private WeakReference<TileDownloadTask> callerTileDownloadTask;

    static final int NUM_RETRIES_WHEN_SERVICE_UNAVAILABLE = 0;
    static final int RETRY_WAIT_MILLIS = 3000;

    public SingleTileDownloadTask(URL tileURL, WeakReference<MainActivity> callerMainActivity, TileDownloadTask callerTileDownloadTask) {
        this.tileURL = tileURL;
        this.callerMainActivity = callerMainActivity;
        this.callerTileDownloadTask = new WeakReference<>(callerTileDownloadTask);
    }

    private void notifyCaller() {
        TileDownloadTask callerTDT = callerTileDownloadTask.get();
        if (callerTDT != null) {
            callerTDT.onSingleThreadFinished();
        }
    }

    @Override
    public Boolean call() {
        try {
            HttpURLConnection connection;
            InputStream input;
            OutputStream output;
            int retries = 0;
            while (true) {
                connection = (HttpURLConnection) this.tileURL.openConnection();
                connection.connect();

                int responseCode = connection.getResponseCode();

                switch (responseCode) {
                    case HttpURLConnection.HTTP_OK:
                        break;

                    case HttpURLConnection.HTTP_NOT_FOUND:
                    case HttpURLConnection.HTTP_UNAVAILABLE:  // 503 - tile server has problems
                        Thread.sleep(RETRY_WAIT_MILLIS);
                        retries += 1;
                        if (retries < NUM_RETRIES_WHEN_SERVICE_UNAVAILABLE) {
                            continue;  // retry
                        } else {
                            notifyCaller();
                            return false;
                        }

                    default:
                        notifyCaller();
                        return false;
                }
                break;
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();

            // download the file
            input = connection.getInputStream();

            MainActivity caller = callerMainActivity.get();
            if (caller == null || caller.isFinishing()) {
                // caller is gone
                notifyCaller();
                return false;
            }

            File tileFile = new File(caller.getFilesDir() + "/tiles/" + tileURL.getFile());

            tileFile.getParentFile().mkdirs();
            tileFile.createNewFile();

            output = new FileOutputStream(tileFile, false);
            byte data[] = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            notifyCaller();
            return true;

        } catch (IOException | InterruptedException e) {
            notifyCaller();
            return false;
        }
    }
}

public class TileDownloadTask extends AsyncTask<Void, Integer, Boolean> {
    private WeakReference<MainActivity> callerMainActivity;
    private List<Tile> tiles;
    public int minZoomLevel, maxZoomLevel;
    public double minLon, minLat, maxLon, maxLat;  // openlayers extent, see https://openlayers.org/en/latest/apidoc/module-ol_extent.html
    boolean missingTilesProbablyDueToInternet = false;
    int threadsFinished = 0;
    ThreadPoolExecutor executor;

    String[] tileserverURLs;

    @Override
    protected void onCancelled() {
        executor.shutdownNow();
        super.onCancelled();
    }

    void initializeTiles() {
        // https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#X_and_Y
        int XYLimit = (int) (Math.pow(2, minZoomLevel) - 1);

        int minXLevel = Math.max(0, getXTileNumber(minLon, minZoomLevel) - 1);
        int maxXLevel = Math.min(XYLimit, getXTileNumber(maxLon, minZoomLevel) + 1);
        int maxYLevel = Math.min(XYLimit, getYTileNumber(minLat, minZoomLevel) + 1);
        int minYLevel = Math.max(0, getYTileNumber(maxLat, minZoomLevel) - 1);

        tiles = new ArrayList<>();

        // Add tiles for outermost zoom level
        addTiles(minZoomLevel, minXLevel, maxXLevel, minYLevel, maxYLevel);

        // Add tiles for more zoomed-in zoom levels
        int x1 = minXLevel;
        int x2 = maxXLevel;
        int y1 = minYLevel;
        int y2 = maxYLevel;
        for (int z = minZoomLevel +1; z <= Math.min(maxZoomLevel, Global.MAX_TILE_ZOOM_LEVEL); z++) {
            x1 = x1*2;
            y1 = y1*2;
            x2 = x2*2+1;
            y2 = y2*2+1;
            addTiles(z, x1, x2, y1, y2);
        }
    }

    public TileDownloadTask(MainActivity caller, int minZoomLevel, int maxZoomLevel, double minLon, double minLat, double maxLon, double maxLat, String[] tileserverURLs) {//, Course course) {
        callerMainActivity = new WeakReference<>(caller);
        this.tileserverURLs = tileserverURLs;
        this.minZoomLevel = minZoomLevel;
        this.maxZoomLevel = maxZoomLevel;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        initializeTiles();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Global.NTHREADS_TILE_DOWNLOAD);
    }

    synchronized void onSingleThreadFinished() {
        threadsFinished += 1;
        publishProgress((threadsFinished*100) / tiles.size());
    }

    void deleteDirectoryRecursively(File root) {
        if (root.isDirectory())
            for (File child : root.listFiles())
                deleteDirectoryRecursively(child);
        root.delete();
    }

    @Override
    protected Boolean doInBackground(Void... v) {
        missingTilesProbablyDueToInternet = false;

        File tileDirectory = new File(callerMainActivity.get().getFilesDir() + "/tiles/");

        // remove all tiles before each download (since course will be replaced -> old tiles redundant)
        deleteDirectoryRecursively(tileDirectory);

        List<SingleTileDownloadTask> taskList = new ArrayList<>();
        int j = 0;
        for (Tile tile: tiles) {
            URL url = null;
            try {
                url = new URL(this.tileserverURLs[j] + tile.z + "/" + tile.x + "/" + tile.y + ".png");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            SingleTileDownloadTask task = new SingleTileDownloadTask(url, callerMainActivity, this);
            taskList.add(task);
            j = (j + 1) % this.tileserverURLs.length;
        }
        threadsFinished = 0;
        List<Future<Boolean>> resultList = null;
        try {
            resultList = executor.invokeAll(taskList);
        } catch (InterruptedException e) {
        }

        executor.shutdown();

        for (int i = 0; i < resultList.size(); i++) {
            Future<Boolean> future = resultList.get(i);
            try {
                Boolean success = future.get();
                if (!success) {

                    MainActivity caller = callerMainActivity.get();
                    if (caller == null || caller.isFinishing()) {
                        // caller is gone
                        return false;
                    }
                    ConnectivityManager cm = (ConnectivityManager) caller.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    boolean hasInternetConnection = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
                    missingTilesProbablyDueToInternet = !hasInternetConnection;
                }
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        MainActivity caller = callerMainActivity.get();
        if (caller == null || caller.isFinishing()) {
            // caller is gone
            return;
        }
        caller.updateProgressBar(values[0]);
    }

    public static int getXTileNumber(final double lon, final int zoom) {
        int xtile = (int)Math.floor( (lon + 180) / 360 * (1<<zoom) ) ;
        if (xtile < 0)
            xtile=0;
        if (xtile >= (1<<zoom))
            xtile=((1<<zoom)-1);
        return xtile;
    }

    public static int getYTileNumber(final double lat, final int zoom) {
        int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
        if (ytile < 0)
            ytile=0;
        if (ytile >= (1<<zoom))
            ytile=((1<<zoom)-1);
        return ytile;
    }

    private void addTiles(int zoom, int x_min, int x_max, int y_min, int y_max) {
        for (int x = x_min; x <= x_max; x++) {
            for (int y = y_min; y <= y_max; y++) {
                tiles.add(new Tile(x, y, zoom));
            }
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        MainActivity caller = callerMainActivity.get();
        if (caller == null || caller.isFinishing()) {
            // caller is gone
            return;
        }
        caller.onTileDownloadTaskFinished(success, missingTilesProbablyDueToInternet);
    }
}
