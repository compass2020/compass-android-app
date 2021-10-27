/*
 * COMPASS orienteering game
 * Copyright (C) 2021 University of Vienna
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>
 */

package at.univie.compass.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import at.univie.compass.R;
import at.univie.compass.dto.ResultCourse;
import at.univie.compass.global.Global;
import at.univie.compass.task.StartupAuthTask;


public class ResultListActivity extends AppCompatActivity implements AuthCallbackInterface {
    // UI
    RecyclerView resultList;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        StartupAuthTask sat = new StartupAuthTask(this, false, false);
        sat.execute();

        // UI
        resultList = findViewById(R.id.resultList);
        resultList.setLayoutManager(new LinearLayoutManager(this));

        // Set up data for list
        ResultCourse[] rcsSent = Global.getStoredViewCodes(getApplicationContext());
        ResultCourse[] rcsUnsent = Global.getStoredResults(getApplicationContext());
        List<ResultCourse> rcList = new ArrayList<>();
        if (rcsSent != null) {
            for (ResultCourse resultCourse : rcsSent) {
                resultCourse.sent = true;
                rcList.add(resultCourse);
            }
        }
        if (rcsUnsent != null) {
            for (ResultCourse resultCourse : rcsUnsent) {
                resultCourse.sent = false;
                rcList.add(resultCourse);
            }
        }
        Collections.reverse(rcList);  // newest first instead of oldest
        resultList.setAdapter(new ResultListAdapter(rcList));
    }

    @Override
    public void onAuthTaskFinished(String result, boolean alsoRecommend, boolean fromScan) {
        if (result.equals(Global.LOGIN_UPDATE_REQUIRED)) {
            Global.authFailedDialog(this, result, alsoRecommend);
        }
    }

    public class ResultListAdapter extends RecyclerView.Adapter<ResultListAdapterViewHolder> {
        private List<ResultCourse> mDataset;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder

        // Provide a suitable constructor (depends on the kind of dataset)
        public ResultListAdapter(List<ResultCourse> myDataset) {
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ResultListAdapterViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
            // create a new view

            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.result_list_item, parent, false);

            ResultListAdapterViewHolder vh = new ResultListAdapterViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ResultListAdapterViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            ResultCourse data = mDataset.get(position);

            // catch Version 1.0.0 case (don't show "old" results) TODO: dont even add them in the first place
            if (data.getSharedCourse() == null || data.getSharedCourse().getCourse() == null || data.getSharedCourse().getCourse().getName() == null || data.getTimeStampStarted() == null) {
                return;
            }

            TextView textView = holder.layout.findViewById(R.id.textView);
            textView.setTextSize(16);

            // date
            Date date = null;
            try {
                SimpleDateFormat informat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
                informat.setTimeZone(TimeZone.getTimeZone("UTC"));
                date = informat.parse(data.getTimeStampStarted());
            } catch (ParseException e) {
                SimpleDateFormat informat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.ENGLISH);
                informat.setTimeZone(TimeZone.getTimeZone("UTC"));
                try {
                    date = informat.parse(data.getTimeStampStarted());
                } catch (ParseException parseException) {
                    parseException.printStackTrace();
                }
            }
            DateFormat df = DateFormat.getDateTimeInstance();
            String dateStr = df.format(date);

            if (!data.sent) {
                String linkText = getString(R.string.course) + ": " + data.getSharedCourse().getCourse().getName() + " (" + dateStr + ")<br>" + getString(R.string.result_not_uploaded_yet);
                textView.setText(Html.fromHtml(linkText));
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                holder.layout.findViewById(R.id.shareButton).setVisibility(View.INVISIBLE);
            } else {
                // url
                url = null;
                try {
                    url = new URL(Global.USE_HTTPS ? "https" : "http", Global.DEFAULT_BACKEND_HOST, Global.BACKEND_PORT, String.format(Global.WEBSITE_VIEW, data.getSharedCourse().getId(), data.getViewCode())).toString();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                String linkText = getString(R.string.course) + ": " + data.getSharedCourse().getCourse().getName() + " (" + dateStr + ")<br>" + getString(R.string.code) + ": " + data.getViewCode() + "<br><a href=\"" + url + "\">" + url + "</a>";
                textView.setText(Html.fromHtml(linkText));
                textView.setMovementMethod(LinkMovementMethod.getInstance());

                View.OnClickListener listener = new View.OnClickListener() {
                    final String urlSaved = url;

                    @Override
                    public void onClick(View v) {
                        Intent sharingIntent = new Intent(
                                android.content.Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        String shareBody = "COMPASS " + data.getSharedCourse().getCourse().getName() + "\n" + urlSaved;
                        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                "COMPASS " + data.getSharedCourse().getCourse().getName());
                        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                                shareBody);
                        startActivity(Intent.createChooser(sharingIntent, "Share via"));
                    }
                };

                holder.layout.findViewById(R.id.shareButton).setOnClickListener(listener);
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }
    }

    public static class ResultListAdapterViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public LinearLayout layout;
        public ResultListAdapterViewHolder(LinearLayout v) {
            super(v);
            layout = v;
        }
    }
}