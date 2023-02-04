package com.yandex.maps.testapp.about;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import java.util.concurrent.ExecutionException;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.experiments.UiExperimentsListener;
import com.yandex.maps.testapp.Environment;
import com.yandex.maps.testapp.MainApplication;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;
import com.yandex.maps.testapp.Utils;
import com.yandex.runtime.Error;
import com.yandex.runtime.init.MiidListener;
import com.yandex.runtime.Runtime;
import com.yandex.maps.auth.AccountFactory;
import com.yandex.maps.recording.RecordingFactory;

public class AboutActivity extends TestAppActivity implements UiExperimentsListener {

    private TextView experimentIdsView;

    private final MiidListener miidListener = new MiidListener() {
        @Override
        public void onMiidReceived(String miid) {
            TextView miidTextView = (TextView)findViewById(R.id.miid);
            miidTextView.setText(miid);
        }
        @Override
        public void onMiidError(Error error) {
            Utils.showError(AboutActivity.this, error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        MapKitFactory.getInstance().getMiidManager().submit(miidListener);

        TextView mapkitVersionTextView = (TextView)findViewById(R.id.mapkit_version);
        mapkitVersionTextView.setText(MapKitFactory.getInstance().getVersion());

        TextView environmentTextView = (TextView)findViewById(R.id.environment_edit);
        environmentTextView.setText(Environment.readEnvironmentFromPreferences(this));

        TextView runtimeVersionTextView = (TextView)findViewById(R.id.runtime_version);
        runtimeVersionTextView.setText(Runtime.getVersion());

        TextView authVersionTextView = (TextView)findViewById(R.id.auth_version);
        authVersionTextView.setText(AccountFactory.getVersion());

        TextView recordingVersionTextView = (TextView)findViewById(R.id.recording_version);
        recordingVersionTextView.setText(RecordingFactory.getInstance().getVersion());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final com.yandex.maps.testapp.DeviceInfo info = ((MainApplication) getApplication()).getDeviceInfo();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (info != null) {
                                ((TextView) findViewById(R.id.uuid)).setText(info.uuid);
                                ((TextView) findViewById(R.id.deviceid)).setText(info.deviceId);
                            } else {
                                ((TextView) findViewById(R.id.uuid)).setText("n/a");
                                ((TextView) findViewById(R.id.deviceid)).setText("n/a");
                            }
                        }
                    });
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        experimentIdsView = (TextView)findViewById(R.id.experiment_ids);
        onParametersUpdated();
        MapKitFactory.getInstance().getUiExperimentsManager().subscribe(this);
    }

    @Override
    protected void onDestroy() {
        MapKitFactory.getInstance().getUiExperimentsManager().unsubscribe(this);
        super.onDestroy();
    }


    @Override
    public void onParametersUpdated() {
        String ids = MapKitFactory.getInstance().getUiExperimentsManager()
                .getValue("test_buckets");
        if (ids == null) {
            experimentIdsView.setText("Waiting...");
        } else {
            experimentIdsView.setText(TextUtils.join("\n", ids.split(";")));
        }
    }

    @Override
    protected void onStartImpl(){}
    @Override
    protected void onStopImpl(){}
}
