package com.yandex.maps.testapp.crashes;

import androidx.annotation.NonNull;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.maps.recording.RecordingFactory;
import com.yandex.maps.recording.UploadSession;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.logs.LogcatSaver;
import com.yandex.runtime.Error;

import java.util.Objects;

public class CrashReportActivity extends TestAppActivity {
    private static UploadSession startrekSession;

    public static void sendCrashReport(Context context, boolean allowCancel) {
        AlertDialog dialog = CrashReportActivity.createCrashReportDialog(context, allowCancel);
        dialog.setCancelable(allowCancel);
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_report);
        if (CrashController.getInstance().needSendCrashReport(this)) {
            CrashReportActivity.sendCrashReport(this, true);
        } else {
            Toast.makeText(this, "Crash reports was not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (startrekSession != null)
            startrekSession.cancel();
    }

    @Override
    protected void onStartImpl() {
    }

    @Override
    protected void onStopImpl() {
    }

    private static UploadSession.UploadListener uploadListener(Context context) {
        return new UploadSession.UploadListener() {
            @Override
            public void onUploadResult(@NonNull String s) {
                Toast.makeText(context, "Crash report was successfully sent: " + s, Toast.LENGTH_SHORT).show();
                CrashReportActivity.startrekSession = null;
            }

            @Override
            public void onUploadError(@NonNull Error error) {
                Toast.makeText(context, "Error on send crash report", Toast.LENGTH_SHORT).show();
                CrashReportActivity.startrekSession = null;
            }
        };
    }

    private static AlertDialog createCrashReportDialog(Context context, boolean allowCancel) {
        final String runUuid = CrashController.getInstance().lastCrashRunUuid(context);
        Objects.requireNonNull(runUuid);

        LayoutInflater inflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.crash_report, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setPositiveButton("Send crash report", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            ((TextView)view.findViewById(R.id.run_uuid)).setText(runUuid);
            if (allowCancel) {
                EditText editText = view.findViewById(R.id.test_case_id_edit);
                editText.setText("0");
                editText.setEnabled(false);
            }

            Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setOnClickListener(view1 -> {
                final String summary = ((EditText)view.findViewById(R.id.summary_edit)).getText().toString();
                if (summary.length() < 5) {
                    Toast.makeText(context, "Summary must be at least 5 symbols", Toast.LENGTH_SHORT).show();
                    return;
                }

                final String description = ((EditText)view.findViewById(R.id.user_description_edit)).getText().toString();

                String testCaseId = ((EditText)view.findViewById(R.id.test_case_id_edit)).getText().toString();
                if (testCaseId.length() < 1) {
                    Toast.makeText(context, "TestCase Id must be at least 1 number", Toast.LENGTH_SHORT).show();
                    return;
                }

                startrekSession = RecordingFactory.getInstance().startrekClient().submitCrashReport(
                        runUuid, summary, description, testCaseId,
                        LogcatSaver.getInstance().getLastCrashLogsPath(), uploadListener(context));
                CrashController.getInstance().markLastCrashAsReported(context);
                dialog.dismiss();
            });
        });

        return dialog;
    }
}
