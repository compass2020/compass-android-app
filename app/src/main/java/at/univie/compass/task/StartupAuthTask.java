/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.task;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

import at.univie.compass.activity.AuthCallbackInterface;
import at.univie.compass.global.Global;

public class StartupAuthTask extends AsyncTask<Void, Void, String> {

    private WeakReference<AuthCallbackInterface> callerRef;
    boolean alsoRecommend, fromScan;

    public StartupAuthTask(AuthCallbackInterface caller, boolean alsoRecommend, boolean fromScan) {
        callerRef = new WeakReference<>(caller);
        this.alsoRecommend = alsoRecommend;
        this.fromScan = fromScan;
    }

    @Override
    protected String doInBackground(Void... voids) {
        String status;
        try {
            status = Global.getAuthenticationToken(Global.BACKEND_USER, Global.BACKEND_USER_PASSWORD, true);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } catch (Exception e) {
            return "";
        }

        return status;
    }

    @Override
    protected void onPostExecute(String s) {
        AuthCallbackInterface caller = callerRef.get();
        if (caller == null || ((Activity) caller).isFinishing())
            // caller is gone
            return;

        caller.onAuthTaskFinished(s, alsoRecommend, fromScan);
    }
}
