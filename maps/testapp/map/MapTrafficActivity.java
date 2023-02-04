package com.yandex.maps.testapp.map;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.yandex.mapkit.traffic.TrafficLevel;
import com.yandex.mapkit.traffic.TrafficListener;
import com.yandex.maps.testapp.R;

import java.util.logging.Logger;

public class MapTrafficActivity extends MapBaseActivity implements TrafficListener {
    private TextView levelText;
    private ImageButton levelIcon;
    private TrafficLevel trafficLevel = null;
    private enum TrafficFreshness {Loading, OK, Expired};
    private TrafficFreshness trafficFreshness;


    private static Logger LOGGER = Logger.getLogger("yandex.maps");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_traffic);
        super.onCreate(savedInstanceState);

        levelText = (TextView) findViewById(R.id.traffic_light_text);
        levelIcon = (ImageButton) findViewById(R.id.traffic_light);
        jamsSwitchBt.setVisibility(View.GONE);

        traffic.setTrafficVisible(true);
        traffic.addTrafficListener(this);
        updateLevel();
    }

    private void updateLevel() {
        int iconId = R.drawable.icon_traffic_light_dark;
        String level = "";
        if (!traffic.isTrafficVisible()) {
            iconId = R.drawable.icon_traffic_light_dark;
        } else if (trafficFreshness == TrafficFreshness.Loading) {
            iconId = R.drawable.icon_traffic_light_violet;
        } else if (trafficFreshness == TrafficFreshness.Expired) {
            iconId = R.drawable.icon_traffic_light_blue;
        } else if (trafficLevel == null) {  // state is fresh but region has no data
            iconId = R.drawable.icon_traffic_light_grey;
        } else {
            switch (trafficLevel.getColor()) {
                case RED: iconId = R.drawable.icon_traffic_light_red; break;
                case GREEN: iconId = R.drawable.icon_traffic_light_green; break;
                case YELLOW: iconId = R.drawable.icon_traffic_light_yellow; break;
                default: iconId = R.drawable.icon_traffic_light_grey; break;
            }
            level = Integer.toString(trafficLevel.getLevel());
        }
        levelIcon.setImageBitmap(BitmapFactory.decodeResource(getResources(), iconId));
        levelText.setText(level);
    }

    public void onLightClick(View view) {
        traffic.setTrafficVisible(!traffic.isTrafficVisible());
        updateLevel();
    }

    public void onClickBack(View view) {
        finish();
    }

    @Override
    public void onTrafficChanged(TrafficLevel trafficLevel) {
        this.trafficLevel = trafficLevel;
        this.trafficFreshness = TrafficFreshness.OK;
        if (trafficLevel != null) {
            LOGGER.info("Traffic level changing: " + Integer.toString(trafficLevel.getLevel()));
        } else {
            LOGGER.info("Traffic level changing: to empty");
        }
        updateLevel();
    }

    @Override
    public void onTrafficLoading() {
        LOGGER.info("Traffic level loading");
        this.trafficLevel = null;
        this.trafficFreshness = TrafficFreshness.Loading;
        updateLevel();
    }

    @Override
    public void onTrafficExpired() {
        LOGGER.info("Traffic level expired");
        this.trafficLevel = null;
        this.trafficFreshness = TrafficFreshness.Expired;
        updateLevel();
    }

}
