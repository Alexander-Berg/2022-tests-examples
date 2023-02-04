package com.yandex.maps.testapp.taxi;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import android.widget.TextView;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.LocalizedValue;
import com.yandex.mapkit.Money;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.taxi.RideInfo;
import com.yandex.mapkit.transport.taxi.RideInfoSession;
import com.yandex.mapkit.transport.taxi.RideInfoSession.RideInfoListener;
import com.yandex.mapkit.transport.taxi.RideOption;
import com.yandex.mapkit.transport.taxi.TaxiManager;

import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.maps.testapp.driving.IconWithLetter;
import com.yandex.maps.testapp.map.MapBaseActivity;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

class RideInfoDisplay {
    private MapObjectCollection mapUserPoints;
    private Context context;
    private TextView rideInfoText;

    MapObjectTapListener tapListener;
    private Map map;


    public RideInfoDisplay(
            Context context,
            Map map,
            TextView rideInfoText) {
        this.context = context;
        this.map = map;
        this.rideInfoText = rideInfoText;
        this.tapListener = tapListener;

        mapUserPoints = map.getMapObjects().addCollection();
    }

    public PlacemarkMapObject addWaypoint(
            Point point, char letter) {
        PlacemarkMapObject placemark = mapUserPoints.addPlacemark(point);
        placemark.setIcon(IconWithLetter.iconWithLetter(letter, Color.RED));
        placemark.setDraggable(false);
        placemark.setZIndex(100);
        return placemark;
    }

    public void setInfoText(String string) {
        rideInfoText.setText(string);
    }

    public void resetWaypoints() {
        mapUserPoints.clear();
    }
}

public class TaxiActivity
        extends MapBaseActivity
        implements InputListener,
        MapObjectTapListener
{
    private static Logger LOGGER = Logger.getLogger("yandex.maps");
    private TaxiManager manager;
    private RideInfoSession session;
    private RideInfoDisplay rideInfoDisplay;
    private RideInfoListener listener;
    private ArrayList<Point> userPoints = new ArrayList<Point>(0);

    private void addUserPoint(Point point) {
        char letter = (char) ('A' + userPoints.size());
        userPoints.add(point);
        rideInfoDisplay.addWaypoint(point, letter);
    }

    private void resetPoints() {
        rideInfoDisplay.resetWaypoints();
        rideInfoDisplay.setInfoText(getString(R.string.taxi_select_start));
        userPoints.clear();
    }

    private void submitRideInfoRequest() {
        cancelSession();
        rideInfoDisplay.setInfoText(getString(R.string.taxi_looking_for_ride));
        session = manager.requestRideInfo(
                userPoints.get(0), userPoints.get(1), listener);
    }

    private void receiveResponse(RideInfo rideInfo) {
        if (rideInfo.getRideOptions().isEmpty()) {
            rideInfoDisplay.setInfoText(getString(R.string.taxi_empty_info));
        } else {
            List<RideOption> options = rideInfo.getRideOptions();
            String result_string = getString(R.string.taxi_options_found);
            result_string += String.valueOf(options.size()) + "\n";
            for (int i = 0; i < options.size(); i++) {
                String minPriceText = options.get(i).getIsMinPrice() ?
                    "(min ride price from source point)" : "";
                result_string += String.valueOf(i) + ": ";
                result_string += options.get(i).getCost().getText() + " ";
                result_string += options.get(i).getWaitingTime().getText() + " ";
                result_string += minPriceText + "\n";
            }
            rideInfoDisplay.setInfoText(result_string);
        }
    }

    private void receiveResponseError(Error error) {
        LOGGER.info("Got ride info error: " + error.toString());
        rideInfoDisplay.setInfoText(String.format(getString(R.string.taxi_error) + ": " + error.toString()) + 
            "\n");
    }

    private void cancelSession() {
        if (session != null) {
            session.cancel();
            session = null;
        }
    }

    private void addPoint(Point point) {
        if (userPoints.size() == 1) {
            addUserPoint(point);
            submitRideInfoRequest();
        } else if (userPoints.size() == 0) {
            addUserPoint(point);
            rideInfoDisplay.setInfoText(getString(R.string.taxi_select_destination));
        } else {
            resetPoints();
            addUserPoint(point);
            rideInfoDisplay.setInfoText(getString(R.string.taxi_select_destination));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.taxi);
        super.onCreate(savedInstanceState);
        manager = TransportFactory.getInstance().createTaxiManager();
        rideInfoDisplay = new RideInfoDisplay(this
            , mapview.getMap(), (TextView)findViewById(R.id.ride_info_box));
        listener = new RideInfoListener() {
            @Override
            public void onRideInfoReceived(RideInfo rideInfo) {
                receiveResponse(rideInfo);
            }
            @Override
            public void onRideInfoError(Error error) {
                receiveResponseError(error);
            }
        };
        mapview.getMap().addInputListener(this);
    }

    public void onClearTap(View view) {
        resetPoints();
        cancelSession();
    }    

    public void onRetryTap(View view) {
        if (session != null) {
            rideInfoDisplay.setInfoText(getString(R.string.taxi_retrying));
            session.retry(listener);
        }
    }

    @Override
    public void onMapTap(Map map, Point position) {
    }

    @Override
    public void onMapLongTap(Map map, Point position) {
        cancelSession();
        addPoint(position);
    }

    @Override
    public boolean onMapObjectTap(MapObject mapObject, Point point) {
        return true;
    }
}
