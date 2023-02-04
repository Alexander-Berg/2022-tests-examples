package com.yandex.maps.testapp.reports;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.yandex.maps.recording.RecordCollector;
import com.yandex.maps.recording.RecordingFactory;
import com.yandex.maps.recording.Report;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.maps.testapp.Utils;

import java.util.List;
import java.util.logging.Logger;

public class ReportsActivity extends TestAppActivity {
    private static Logger LOGGER = Logger.getLogger("com.yandex.maps.testapp.reports");

    private ListView reportsList;
    private ArrayAdapter<Report> reportsAdapter;

    private RecordCollector collector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reports);
        reportsList = findViewById(R.id.reports_list);
        collector = RecordingFactory.getInstance().recordCollector();
    }

    @Override
    protected void onStart() {
        super.onStart();
        showReports();
    }

    @Override
    protected void onStopImpl(){}
    @Override
    protected void onStartImpl(){}

    private void showReports() {
        List<Report> reports = collector.reports();
        reportsAdapter = new ArrayAdapter<Report>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                reports) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                Report report = getItem(position);
                text1.setText(report.getDisplayName());
                if (report.getIssueId() != null) {
                    text1.setText(report.getDisplayName() + " ✓");
                }
                text2.setText(report.isHasMarkedProblem() ? "has problem" : "");
                return view;
            }
        };
        reportsList.setAdapter(reportsAdapter);
        reportsList.setClickable(true);
        reportsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (AuthUtil.getCurrentAccount() == null) {
                    Utils.showMessage(getBaseContext(), "You should login to send report");
                    return;
                }
                Report report = reportsAdapter.getItem(position);
                if (report.getIssueId() != null) {
                    Utils.showMessage(getBaseContext(), "You already sent this report. Issue ID - " + report.getIssueId());
                    return;
                }
                LOGGER.info("on click report #" + Integer.toString(position) + ": " + report.getDisplayName());
                showSubmitActivity(position);
            }
        });
    }

    private void showSubmitActivity(int position) {
        Intent intent = new Intent(this, ReportSubmitActivity.class);
        intent.putExtra(ReportSubmitActivity.reportIdExtra, Integer.toString(position));
        startActivity(intent);
    }
}
