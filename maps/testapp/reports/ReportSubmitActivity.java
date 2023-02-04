package com.yandex.maps.testapp.reports;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import com.yandex.maps.recording.RecordingFactory;
import com.yandex.maps.recording.Report;
import com.yandex.maps.recording.UploadSession;
import com.yandex.maps.recording.Service;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.passport.api.PassportAccount;
import com.yandex.runtime.Error;

public class ReportSubmitActivity extends Activity {
    public static final String reportIdExtra = "report";

    private Report report;
    private TextView statusText;
    private EditText descriptionEdit;
    private UploadSession session;
    private PassportAccount account;
    private String summary;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_submit);
        statusText = (TextView) findViewById(R.id.reports_status_text);
        TextView summaryText = (TextView) findViewById(R.id.reports_summary_text);
        descriptionEdit = (EditText) findViewById(R.id.reports_description_edit);
        int reportId = Integer.parseInt(getIntent().getExtras().getString(reportIdExtra));
        report = RecordingFactory.getInstance().recordCollector().reports().get(reportId);
        account = AuthUtil.getCurrentAccount();
        String accountSuffix = account == null ? "" : (" from " + account.getPrimaryDisplayName());
        if (account == null) {
            statusText.setText("Error: not logged in...");
        }
        summary = "Report " + report.getDisplayName() + accountSuffix;
        summaryText.setText("Summary: " + summary);
        submitButton = (Button)findViewById(R.id.reports_button_submit);
    }

    public void onSubmit(View view) {
        String description = descriptionEdit.getText().toString();
        if (session != null) {
            submitButton.setText("Submit");
            statusText.setText("Cancelled");
            session.cancel();
            session = null;
            return;
        }
        submitButton.setText("Cancel");
        statusText.setText("Submitting...");
        session = RecordingFactory.getInstance().startrekClient().submitReport(
                summary, description, Service.GUIDANCE, report,
                new UploadSession.UploadListener() {
            @Override
            public void onUploadResult(String s) {
                statusText.setText("OK: " + s);
                submitButton.setText("Submit");
                submitButton.setEnabled(false);
                session = null;
            }

            @Override
            public void onUploadError(Error error) {
                if(error instanceof com.yandex.runtime.network.ForbiddenError) {
                    statusText.setText("Error: Forbidden");
                    submitButton.setEnabled(false);
                }
                else if(error instanceof com.yandex.maps.recording.AlreadySent) {
                    statusText.setText("Error: Report has been alredy sent");
                    submitButton.setEnabled(false);
                }
                else {
                    statusText.setText("Error: " + error.getClass().getName());
                }
                submitButton.setText("Submit");
                session = null;
            }
        });
    }
}
