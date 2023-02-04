package com.yandex.maps.testapp.directions_navigation;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.maps.recording.DownloadSession;
import com.yandex.maps.recording.RecordingFactory;
import com.yandex.maps.recording.Service;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.recording.ReportData;
import com.yandex.runtime.recording.ReportFactory;

public class ReportFetchActivity extends Activity {

    private TextView statusText;
    private EditText issueIdEdit;
    private DownloadSession session;
    private Button downloadButton;
    private ReportFactory reportFactory;
    private static byte[] downloadedReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fetch_report);
        statusText = findViewById(R.id.download_status_text);
        issueIdEdit = findViewById(R.id.issue_id_edit);
        downloadButton = findViewById(R.id.download_report_button);
        reportFactory = DirectionsFactory.getInstance().createReportFactory();
    }

    public void onDownload(View view) {
        if (session != null) {
            statusText.setText("Cancelled");
            downloadButton.setText("Download");
            session.cancel();
            session = null;
            return;
        }
        String issueId = issueIdEdit.getText().toString().toUpperCase();
        downloadButton.setText("Cancel");
        statusText.setText("Downloading report...");

        session = RecordingFactory.getInstance().startrekClient().fetchReport(
                issueId, Service.GUIDANCE, reportFactory,
                new DownloadSession.DownloadListener() {
                    @Override
                    public void onDownloadResult(ReportData report) {
                        Intent intent = new Intent();
                        session = null;
                        if (report != null) {
                            statusText.setText("OK");
                            // We can't use intent here because report is too large:
                            // TransactionTooLargeException
                            downloadedReport = report.data();
                            setResult(RESULT_OK, intent);
                            finish();
                        } else {
                            statusText.setText("Downloaded report is empty");
                            downloadButton.setText("Retry");
                        }
                    }

                    @Override
                    public void onDownloadError(com.yandex.runtime.Error error) {
                        statusText.setText("Error: " + error.getClass().getName());
                        downloadButton.setText("Retry");
                        session = null;
                    }
                });
    }

    public static byte[] getDownloadedReport() {
        return downloadedReport;
    }
}
