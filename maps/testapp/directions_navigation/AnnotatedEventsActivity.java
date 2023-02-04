package com.yandex.maps.testapp.directions_navigation;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.yandex.mapkit.directions.navigation.AnnotatedEvents;
import com.yandex.mapkit.directions.navigation.AnnotatedRoadEvents;
import com.yandex.maps.testapp.R;

import java.util.ArrayList;
import java.util.List;

public class AnnotatedEventsActivity extends Activity {
    private final List<Pair<Integer, String>> annotatedEventsList = new ArrayList<Pair<Integer, String>>() {{
        add(new Pair<>(AnnotatedEvents.MANOEUVRES.value, "Manoeuvres"));
        add(new Pair<>(AnnotatedEvents.FASTER_ALTERNATIVE.value, "Faster Alternative"));
        add(new Pair<>(AnnotatedEvents.ROAD_EVENTS.value, "Road Events"));
        add(new Pair<>(AnnotatedEvents.TOLL_ROAD_AHEAD.value, "Toll Road Ahead"));
        add(new Pair<>(AnnotatedEvents.SPEED_LIMIT_EXCEEDED.value, "Speed Limit Exceeded"));
        add(new Pair<>(AnnotatedEvents.PARKING_ROUTES.value, "Parking Routes"));
        add(new Pair<>(AnnotatedEvents.STREETS.value, "Streets"));
        add(new Pair<>(AnnotatedEvents.ROUTE_STATUS.value, "Route Status"));
        add(new Pair<>(AnnotatedEvents.WAY_POINTS.value, "Way Points"));
        add(new Pair<>(AnnotatedEvents.SPEED_BUMPS.value, "Speed Bumps"));
        add(new Pair<>(AnnotatedEvents.RAILWAY_CROSSINGS.value, "Railway Crossings"));
    }};

    private final List<Pair<Integer, String>> annotatedRoadEventsList = new ArrayList<Pair<Integer, String>>() {{
        add(new Pair<>(AnnotatedRoadEvents.DANGER.value, "Danger"));
        add(new Pair<>(AnnotatedRoadEvents.RECONSTRUCTION.value, "Reconstruction"));
        add(new Pair<>(AnnotatedRoadEvents.ACCIDENT.value, "Accident"));
        add(new Pair<>(AnnotatedRoadEvents.SCHOOL.value, "School"));
        add(new Pair<>(AnnotatedRoadEvents.OVERTAKING_DANGER.value, "Overtaking Danger"));
        add(new Pair<>(AnnotatedRoadEvents.PEDESTRIAN_DANGER.value, "Pedestrian Danger"));
        add(new Pair<>(AnnotatedRoadEvents.CROSS_ROAD_DANGER.value, "Crossroad Danger"));
        add(new Pair<>(AnnotatedRoadEvents.LANE_CONTROL.value, "Lane Control"));
        add(new Pair<>(AnnotatedRoadEvents.ROAD_MARKING_CONTROL.value, "Road Marking Control"));
        add(new Pair<>(AnnotatedRoadEvents.CROSS_ROAD_CONTROL.value, "Crossroad Control"));
        add(new Pair<>(AnnotatedRoadEvents.MOBILE_CONTROL.value, "Mobile Control"));
        add(new Pair<>(AnnotatedRoadEvents.SPEED_LIMIT_CONTROL.value, "Speed Limit Control"));
    }};

    private Intent resultIntent;
    private int annotatedEvents;
    private int annotatedRoadEvents;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.directions_navigation_annotated_events);
        super.onCreate(savedInstanceState);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(view -> finish());

        Intent intent = getIntent();
        annotatedEvents = intent.getIntExtra("annotatedEvents", 0);
        annotatedRoadEvents = intent.getIntExtra("annotatedRoadEvents", 0);

        initAnnotatedEvents();
        initAnnotatedRoadEvents();

        resultIntent = new Intent();
        resultIntent.putExtra("annotatedEvents", annotatedEvents);
        resultIntent.putExtra("annotatedRoadEvents", annotatedRoadEvents);
        setResult(Activity.RESULT_OK, resultIntent);
    }

    private void initAnnotatedEvents() {
        LinearLayout annotatedEventsLayout = findViewById(R.id.annotated_events_layout);
        for (Pair<Integer, String> event : annotatedEventsList) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setChecked(isEventAnnotated(annotatedEvents, event.first));
            checkBox.setText(event.second);
            checkBox.setOnClickListener(view -> {
                boolean checked = ((CheckBox) view).isChecked();
                annotatedEvents = setEventAnnotated(annotatedEvents, event.first, checked);
                resultIntent.putExtra("annotatedEvents", annotatedEvents);
            });
            annotatedEventsLayout.addView(checkBox);
        }
    }

    private void initAnnotatedRoadEvents() {
        LinearLayout annotatedRoadEventsLayout = findViewById(R.id.annotated_road_events_layout);
        for (Pair<Integer, String> event : annotatedRoadEventsList) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setChecked(isEventAnnotated(annotatedRoadEvents, event.first));
            checkBox.setText(event.second);
            checkBox.setOnClickListener(view -> {
                boolean checked = ((CheckBox) view).isChecked();
                annotatedRoadEvents = setEventAnnotated(annotatedRoadEvents, event.first, checked);
                resultIntent.putExtra("annotatedRoadEvents", annotatedRoadEvents);
            });
            annotatedRoadEventsLayout.addView(checkBox);
        }
    }

    private static boolean isEventAnnotated(int annotatedEvents, int event) {
        return (annotatedEvents & event) == event;
    }

    private static int setEventAnnotated(int annotatedEvents, int event, boolean annotated) {
        return annotated
                ? annotatedEvents | event
                : annotatedEvents & ~event;
    }
}
