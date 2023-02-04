package com.yandex.maps.testapp.tests;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

import java.util.ArrayList;
import java.util.List;

public class NetworkTestActivity extends TestAppActivity implements View.OnClickListener {

    private static final String TAG = NetworkTestActivity.class.getCanonicalName();
    private Spinner testChooserSpinner;
    private DrivingRouter router;
    private ArrayList<RequestPoint> points;
    private TextView testTitleView;
    private int currentTestId;
    private double minTime;
    private double maxTime;
    private double totalTime;
    private EditText requestCountField;
    private DrivingSession drivingSession;
    private long startTime;
    private EditText requestTimeoutField;
    private TextView minTimeView;
    private TextView maxTimeView;
    private TextView averageTimeView;
    private TextView totalTimeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_test);
        testChooserSpinner = (Spinner) findViewById(R.id.testChooser);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.network_tests, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        testChooserSpinner.setAdapter(adapter);

        ((Button)findViewById(R.id.runTestButton)).setOnClickListener(this);

        router = DirectionsFactory.getInstance().createDrivingRouter();
        points = new ArrayList<RequestPoint>();
        points.add(new RequestPoint(new Point(39.990466, 32.795853),
                RequestPointType.WAYPOINT,
                null /* pointContext */));
        points.add(new RequestPoint(new Point(39.884497, 32.863583),
                RequestPointType.WAYPOINT,
                null /* pointContext */));


        testTitleView = (TextView) findViewById(R.id.networkTest);
        requestCountField = (EditText) findViewById(R.id.requestCoundField);
        requestTimeoutField = (EditText) findViewById(R.id.requestTimeoutField);
        minTimeView = (TextView) findViewById(R.id.requestMinTime);
        maxTimeView = (TextView) findViewById(R.id.requestMaxTime);
        averageTimeView = (TextView) findViewById(R.id.requestAverageTime);
        totalTimeView = (TextView) findViewById(R.id.requestTotalTime);
    }

    @Override
    public void onClick(View v) {
        currentTestId = 0;
        totalTime = 0;
        minTime = 10000;
        maxTime = 0;
        executeTest();
    }

    private void executeTest() {
        String text = "Running Test1: " + currentTestId + " of " + requestCountField.getText();
        testTitleView.setText(text);
        Log.e(TAG, text);

        startTime = System.currentTimeMillis();

        int testId = testChooserSpinner.getSelectedItemPosition();
        switch (testId) {
            case 0:
                drivingTest();
                break;
            default:
                break;
        }
    }
    private void testCompleted() {
        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;
        Log.e(TAG, "Execution Time: " + executionTime + " for request " + currentTestId +
                ", requestTimeout " + requestCountField.getText());
        if (executionTime < minTime) {
            minTime = executionTime;
        }
        if (executionTime > maxTime) {
            maxTime = executionTime;
        }
        totalTime += executionTime;
        ++currentTestId;
        int totalRequest = Integer.parseInt(requestCountField.getText().toString());

        if (currentTestId < totalRequest) {
            long requestTimeout = Long.parseLong(requestTimeoutField.getText().toString());
            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    executeTest();
                }
            }, requestTimeout);
        } else {
            String tmpText = String.format("Average time: %.4f", totalTime / totalRequest);
            averageTimeView.setText(tmpText);
            Log.e(TAG, tmpText);

            tmpText = String.format("Min time: %.4f", minTime);
            minTimeView.setText(tmpText);
            Log.e(TAG, tmpText);

            tmpText = String.format("Max time: %.4f", maxTime);
            maxTimeView.setText(tmpText);
            Log.e(TAG, tmpText);

            tmpText = "Test1 Completed";
            testTitleView.setText(tmpText);
            Log.e(TAG, tmpText);

            tmpText = String.format("Total time: %.4f", totalTime);
            totalTimeView.setText(tmpText);
            Log.e(TAG, tmpText);
        }
    }

    private void drivingTest() {
        drivingSession = router.requestRoutes(points, new DrivingOptions(), new VehicleOptions(),
                new DrivingSession.DrivingRouteListener() {
                    @Override
                    public void onDrivingRoutes(List<DrivingRoute> list) {
                        Log.e(TAG, "onDrivingRoutes: list.size = " + list.size());
                        if (list.size() > 0) {
                            Log.e(TAG, "route distance: " + list.get(0).getMetadata().getWeight().getDistance().getText() +
                             " , time: " + list.get(0).getMetadata().getWeight().getTime().getText());
                        }
                        testCompleted();
                    }

                    @Override
                    public void onDrivingRoutesError(com.yandex.runtime.Error error) {
                        Log.e(TAG, "onDrivingRoutesError: " + error.toString());
                        testCompleted();
                    }
                });
    }

    @Override
    protected void onStartImpl(){}
    @Override
    protected void onStopImpl(){}
}
