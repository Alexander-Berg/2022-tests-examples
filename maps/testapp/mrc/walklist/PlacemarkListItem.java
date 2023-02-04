package com.yandex.maps.testapp.mrc.walklist;

import com.yandex.maps.testapp.search.test.Place;
import com.yandex.mrc.BriefRideInfo;
import com.yandex.mrc.LocalRide;
import com.yandex.mrc.ServerRide;
import com.yandex.mrc.walk.LocalPlacemark;
import com.yandex.mrc.walk.PlacemarkData;
import com.yandex.mrc.walk.ServerPlacemark;

public class PlacemarkListItem {
    private LocalPlacemark localPlacemark = null;
    private ServerPlacemark serverPlacemark = null;
    private PlacemarkData placemarkData = null;

    public PlacemarkListItem(LocalPlacemark placemark) {
        this.localPlacemark = placemark;
        this.placemarkData = placemark.getData();
    }

    public PlacemarkListItem(ServerPlacemark placemark) {
        this.serverPlacemark = placemark;
        this.placemarkData = placemark.getData();
    }

    public PlacemarkData getData() {
        return placemarkData;
    }

    public boolean hasLocalPlacemark() {
        return localPlacemark != null;
    }

    public boolean hasServerPlacemark() {
        return serverPlacemark != null;
    }

    public LocalPlacemark getLocalPlacemark() {
        return localPlacemark;
    }

    public ServerPlacemark getServerPlacemark() {
        return serverPlacemark;
    }

    public String getId() {
        if (hasLocalPlacemark()) {
            return localPlacemark.getData().getId();
        } else if (hasServerPlacemark()) {
            return serverPlacemark.getData().getId();
        } else {
            return null;
        }
    }
}
