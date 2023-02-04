package com.yandex.maps.testapp.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.GeoObjectTapEvent;
import com.yandex.mapkit.layers.GeoObjectTapListener;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.mapkit.traffic.RoadEventTapInfo;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.bindings.Serialization;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.map.CameraPosition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

public class MapRoadEventsActivity extends MapBaseActivity implements GeoObjectTapListener, InputListener {
    private final static int REQUEST_CODE_TAGS = 1;
    private static Logger LOGGER = Logger.getLogger("yandex.maps");

    public static final String pointExtra = "point";
    public static final String eventIdExtra = "event";
    private UserLocationLayer userLocationLayer = null;

    private HashMap<Integer, EventTag> tagsMap;

    private class BriefCard {
        private LinearLayout briefCardLayout;
        private TextView id;
        private TextView tags;

        public BriefCard () {
            briefCardLayout = findViewById(R.id.road_event_brief_card);
            id = findViewById(R.id.road_event_brief_card_id);
            tags = findViewById(R.id.road_event_brief_card_tags);
        }

        public void hide() {
            briefCardLayout.setVisibility(android.view.View.GONE);
        }

        public void show(final RoadEventTapInfo info) {
            id.setText("Id: " + info.getId());

            StringBuilder builder = new StringBuilder();
            for (EventTag tag : info.getTags()) {
                builder.append(tag.name());
                builder.append(",");
            }
            String tagsString = builder.toString();

            tags.setText("Tags: " + tagsString);

            briefCardLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showFullCard(info.getId());
                }
            });
            briefCardLayout.setVisibility(android.view.View.VISIBLE);
        }
    }

    private BriefCard briefCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_road_events);
        super.onCreate(savedInstanceState);

        briefCard = new BriefCard();
        mapview.getMap().addTapListener(this);
        mapview.getMap().addInputListener(this);

        userLocationLayer =
            MapKitFactory.getInstance()
                .createUserLocationLayer(mapview.getMapWindow());

        userLocationLayer.setVisible(true);
        final Button moveToLocationButton = (Button)findViewById(R.id.findMeButton);
        moveToLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraPosition position = userLocationLayer.cameraPosition();
                if (position != null)
                    mapview.getMap().move(position);
            }
        });

        setAllRoadEventsVisible();
    }

    private void showFullCard(String eventId) {
        Intent intent = new Intent(this, MapRoadEventFullCardActivity.class);
        intent.putExtra(eventIdExtra, eventId);
        startActivity(intent);
    }

    public void onClickBack(View view) {
        finish();
    }

    private void setAllRoadEventsVisible() {
        for (EventTag tag : EventTag.values()) {
            traffic.setRoadEventVisible(tag, true);
        }
    }

    private RoadEventsTagsState getTagsState() {
        boolean[] tagEnabled = new boolean[EventTag.values().length];
        for (int i = 0; i < EventTag.values().length; i++) {
            tagEnabled[i] = traffic.isRoadEventVisible(EventTag.values()[i]);
        }
        return new RoadEventsTagsState(tagEnabled);
    }

    public void onTagsClick(View view) {
        Intent intent = new Intent(this, MapRoadEventsTagsActivity.class);
        intent.putExtra("RoadEventsTagsState", getTagsState());
        intent.putExtra("DisabledRoadEventTags", new RoadEventTagsList(Arrays.asList(EventTag.LOCAL_CHAT)));
        startActivityForResult(intent, REQUEST_CODE_TAGS);
    }

    @Override
    public void onMapTap(Map map, Point position) {
        mapview.getMap().deselectGeoObject();
        briefCard.hide();
    }

    @Override
    public void onMapLongTap(Map map, final Point position) {
        Intent intent = new Intent(MapRoadEventsActivity.this, MapAddRoadEventActivity.class);
        intent.putExtra(pointExtra, Serialization.serializeToBytes(position));
        intent.putExtra("Tags", new RoadEventTagsList(Arrays.asList(
                EventTag.SPEED_CONTROL,
                EventTag.ACCIDENT,
                EventTag.RECONSTRUCTION,
                EventTag.CHAT,
                EventTag.OTHER)));
        startActivity(intent);
    }

    @Override
    public boolean onObjectTap(GeoObjectTapEvent event) {
        RoadEventTapInfo info = event.getGeoObject().getMetadataContainer().getItem(RoadEventTapInfo.class);
        if (info == null) {
            return false;
        }

        event.setSelected(true);
        briefCard.show(info);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);

        switch (requestCode) {
            case REQUEST_CODE_TAGS:
                if(resultCode == RESULT_OK) {
                    RoadEventsTagsState tagsState = dataIntent.getParcelableExtra("RoadEventsTagsState");
                    for (EventTag tag : EventTag.values()) {
                        traffic.setRoadEventVisible(tag, tagsState.isTagEnabled(tag));
                    }
                }
        }
    }
}
