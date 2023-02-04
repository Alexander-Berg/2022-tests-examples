package com.yandex.maps.testapp.map;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.yandex.mapkit.coverage.Coverage;
import com.yandex.mapkit.coverage.Region;
import com.yandex.mapkit.coverage.RegionsSession;
import com.yandex.mapkit.coverage.RegionsSession.RegionsListener;
import com.yandex.mapkit.geometry.LinearRing;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.VisibleRegion;
import com.yandex.runtime.Error;

public class RegionIdWidget {

    private TextView regionIdView;
    private Map map;
    private Coverage coverage;
    private RegionsSession coverageSession;
    private String unavailableText;
    private String errorText;

    private final RegionsListener regionsListener = new RegionsListener() {
        public void onRegionsResponse(List<Region> regions) {
            if (!regions.isEmpty()) {
                regionIdView.setText(Integer.toString(regions.get(0).getId()));
            } else {
                regionIdView.setText(unavailableText);
            }
        }

        public void onRegionsError(Error error) {
            regionIdView.setText(errorText);
        }
    };

    private final CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraPositionChanged(Map map, CameraPosition position,
                CameraUpdateReason updateReason, boolean finished) {
            if (finished) {
                requestRegionId(position);
            }
        }
    };

    RegionIdWidget(TextView regionIdView,
            Coverage coverage,
            Map map,
            String unavailableText,
            String errorText) {

        this.regionIdView = regionIdView;
        this.coverage = coverage;
        this.map = map;
        this.unavailableText = unavailableText;
        this.errorText = errorText;

        map.addCameraListener(cameraListener);
        requestRegionId(map.getCameraPosition());
    }

    private void requestRegionId(CameraPosition position) {
        if (coverageSession != null) {
            coverageSession.cancel();
        }

        coverageSession = coverage.regions(
                position.getTarget(),
                Math.round(position.getZoom()),
                regionsListener);

        VisibleRegion region = map.visibleRegion(position);
        ArrayList<Point> points = new ArrayList<Point>();
        points.add(region.getTopLeft());
        points.add(region.getTopRight());
        points.add(region.getBottomRight());
        points.add(region.getBottomLeft());
        points.add(region.getTopLeft());
        LinearRing area = new LinearRing(points);
        coverage.setActiveArea(area, Math.round(position.getZoom()));
    }
}

