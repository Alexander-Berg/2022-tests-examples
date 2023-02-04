package com.yandex.maps.testapp.mrc.ridelist;

import com.yandex.mrc.BriefRideInfo;
import com.yandex.mrc.LocalRide;
import com.yandex.mrc.ServerRide;

public class RideListItem {
    private LocalRide localRide = null;
    private ServerRide serverRide = null;
    private BriefRideInfo briefRideInfo = null;

    public RideListItem(LocalRide localRide) {
        this.localRide = localRide;
        this.briefRideInfo = localRide.getBriefRideInfo();
    }

    public RideListItem(ServerRide serverRide) {
        this.serverRide = serverRide;
        this.briefRideInfo = serverRide.getBriefRideInfo();
    }

    public RideListItem(LocalRide localRide, ServerRide serverRide, BriefRideInfo mergedRideInfo)
    {
        this.serverRide = serverRide;
        this.localRide = localRide;
        this.briefRideInfo = mergedRideInfo;
    }

    public boolean hasLocalRide() {
        return localRide != null;
    }

    public boolean hasServerRide() {
        return serverRide != null;
    }

    public LocalRide getLocalRide() {
        return localRide;
    }

    public ServerRide getServerRide() {
        return serverRide;
    }

    public BriefRideInfo getBriefInfo() {
        return briefRideInfo;
    }

    public int getLocalPhotosCount() {
        return hasLocalRide() ? localRide.getLocalPhotosCount() : 0;
    }

    public String getRideId() {
        if (hasLocalRide()) {
            return localRide.getBriefRideInfo().getId();
        } else if (hasServerRide()) {
            return serverRide.getBriefRideInfo().getId();
        } else {
            return null;
        }
    }
}
