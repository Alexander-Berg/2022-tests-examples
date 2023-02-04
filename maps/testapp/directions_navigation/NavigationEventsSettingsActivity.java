package com.yandex.maps.testapp.directions_navigation;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import com.yandex.mapkit.road_events.EventTag;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

import java.util.Arrays;
import java.util.EnumMap;

public class NavigationEventsSettingsActivity extends TestAppActivity {
    enum EventDomain {
        Layer,
        Route
    }

    private CheckBox showRoadEventsOnRouteView;
    private CheckBox showRoadEventsOutsideRouteView;
    private ToggleButton showInLayerTagsButton;
    private ToggleButton showOnRouteTagsButton;
    private LinearLayout tagsLayout;
    private Intent resultIntent;
    private EnumMap<EventTag, CheckedTextView> tagViewByTag = new EnumMap(EventTag.class);
    private EventDomain currentTagsDomain;

    @NonNull
    private boolean[] extractFlags(@NonNull Intent intent, int nameResourceId)
    {
        boolean[] tagFlags = intent.getBooleanArrayExtra(getString(nameResourceId));
        if (tagFlags == null) {
            tagFlags = new boolean[EventTag.values().length];
            Arrays.fill(tagFlags, true);
        }
        return tagFlags;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_events_settings);

        showRoadEventsOnRouteView = findViewById(R.id.show_road_events_on_route);
        showRoadEventsOutsideRouteView = findViewById(R.id.show_road_events_outside_route);
        showInLayerTagsButton = findViewById(R.id.show_in_layer_tags);
        showOnRouteTagsButton = findViewById(R.id.show_on_route_tags);
        tagsLayout = findViewById(R.id.tags_layout);
        Intent requestIntent = getIntent();
        Boolean showRoadEventsOnRoute = requestIntent.getBooleanExtra(
                getString(R.string.extra_show_road_events_on_route), true
        );
        Boolean showRoadEventsOutsideRoute = requestIntent.getBooleanExtra(
                getString(R.string.extra_show_road_events_outside_route), false
        );

        showRoadEventsOnRouteView.setChecked(showRoadEventsOnRoute);
        showRoadEventsOutsideRouteView.setChecked(showRoadEventsOutsideRoute);

        for (EventTag tag: EventTag.values()) {
            CheckedTextView view = new CheckedTextView(new ContextThemeWrapper(this,
                    R.style.SettingsCheckedView));
            view.setText(tag.toString());
            tagsLayout.addView(view);
            tagViewByTag.put(tag, view);
        }

        resultIntent = new Intent();
        boolean[] inLayerTagFlags = extractFlags(requestIntent, R.string.extra_road_events_in_layer_shown_tags);
        resultIntent.putExtra(getString(R.string.extra_road_events_in_layer_shown_tags), inLayerTagFlags);
        boolean[] onRouteTagFlags = extractFlags(requestIntent, R.string.extra_road_events_on_route_shown_tags);
        resultIntent.putExtra(getString(R.string.extra_road_events_on_route_shown_tags), onRouteTagFlags);
        resultIntent.putExtra(getString(R.string.extra_show_road_events_on_route), showRoadEventsOnRoute);
        resultIntent.putExtra(getString(R.string.extra_show_road_events_outside_route), showRoadEventsOutsideRoute);

        setResult(RESULT_OK, resultIntent);

        setEventTagsDomain(EventDomain.Layer);
    }

    void showEventTags() {
        int tagsNameResourceId = getTagsNameResourceId(currentTagsDomain);
        boolean[] flags = extractFlags(resultIntent, tagsNameResourceId);
        for (EventTag tag: EventTag.values()) {
            CheckedTextView view = tagViewByTag.get(tag);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckedTextView check = (CheckedTextView) v;
                    check.toggle();
                    boolean[] tagFlags = extractFlags(resultIntent, tagsNameResourceId);
                    tagFlags[tag.ordinal()] = check.isChecked();
                    resultIntent.putExtra(getString(tagsNameResourceId), tagFlags);
                }
            });
            view.setChecked(flags[tag.ordinal()]);
        }
    }

    int getTagsNameResourceId(EventDomain domain) {
        switch (domain) {
            case Layer:
                return R.string.extra_road_events_in_layer_shown_tags;
            case Route:
                return R.string.extra_road_events_on_route_shown_tags;
            default:
                throw new RuntimeException("Unexpected road event domain");
        }
    }

    void setEventTagsDomain(EventDomain domain) {
        currentTagsDomain = domain;
        showEventTags();
        switch (domain) {
            case Layer:
                showInLayerTagsButton.setChecked(true);
                showOnRouteTagsButton.setChecked(false);
                break;
            case Route:
                showInLayerTagsButton.setChecked(false);
                showOnRouteTagsButton.setChecked(true);
                break;
        }
    }

    public void onShowRoadEventsOnRouteClicked(View view) {
        resultIntent.putExtra(
                getString(R.string.extra_show_road_events_on_route), showRoadEventsOnRouteView.isChecked());
    }

    public void onShowRoadEventsOutsideRouteClicked(View view) {
        resultIntent.putExtra(
                getString(R.string.extra_show_road_events_outside_route), showRoadEventsOutsideRouteView.isChecked());
    }

    void setAllEventTags(boolean value) {
        boolean[] tagFlags = new boolean[EventTag.values().length];
        Arrays.fill(tagFlags, value);
        resultIntent.putExtra(getString(getTagsNameResourceId(currentTagsDomain)), tagFlags);
        showEventTags();
    }

    public void onDisableAllTagsClicked(View view) {
        setAllEventTags(false);
    }

    public void onEnableAllTagsClicked(View view) {
        setAllEventTags(true);
    }

    public void onShowInLayerTagsClicked(View view) {
        setEventTagsDomain(EventDomain.Layer);
    }

    public void onShowOnRouteTagsClicked(View view) {
        setEventTagsDomain(EventDomain.Route);
    }

    @Override
    protected void onStopImpl(){}
    @Override
    protected void onStartImpl(){}
}
