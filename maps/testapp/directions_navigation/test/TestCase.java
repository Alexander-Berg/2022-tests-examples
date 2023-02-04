package com.yandex.maps.testapp.directions_navigation.test;

import android.os.Parcel;
import android.os.Parcelable;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.driving.VehicleType;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestCase implements Parcelable {
    public List<RequestPoint> routePoints;
    public List<RequestPoint> simulationRoutePoints;
    public String routeUri;
    public String simulationRouteUri;
    public RouteSelector.SelectorType selectorType;
    public VehicleOptions vehicleOptions;
    public boolean useParkingRoutes;
    public boolean disableAlternatives;
    public boolean freeDrive;

    public TestCase(List<RequestPoint> routePoints,
                    List<RequestPoint> simulationRoutePoints,
                    String routeUri,
                    String simulationRouteUri,
                    RouteSelector.SelectorType selectorType,
                    VehicleOptions vehicleOptions,
                    boolean useParkingRoutes,
                    boolean disableAlternatives,
                    boolean freeDrive) {
        this.routePoints = routePoints == null
                ? Collections.emptyList()
                : routePoints;
        this.simulationRoutePoints = simulationRoutePoints == null
                ? Collections.emptyList()
                : simulationRoutePoints;
        this.routeUri = routeUri == null ? new String() : routeUri;
        this.simulationRouteUri = simulationRouteUri == null ? new String() : simulationRouteUri;
        this.selectorType = selectorType;
        this.vehicleOptions = vehicleOptions;
        this.useParkingRoutes = useParkingRoutes;
        this.disableAlternatives = disableAlternatives;
        this.freeDrive = freeDrive;
    }

    public boolean hasRoute() {
        return routeUri != null || (routePoints != null && routePoints.size() >= 2);
    }

    public boolean hasSimulationRoute() {
        return simulationRouteUri != null ||
                (simulationRoutePoints != null && simulationRoutePoints.size() >= 2);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private void writePointsArray(Parcel out, List<RequestPoint> points) {
        out.writeInt(points.size());
        for (RequestPoint point : points) {
            out.writeDouble(point.getPoint().getLatitude());
            out.writeDouble(point.getPoint().getLongitude());
            out.writeString(point.getType().toString());
            out.writeString(point.getPointContext());
        }
    }

    private List<RequestPoint> readPointsArray(Parcel in) {
        int size = in.readInt();
        ArrayList<RequestPoint> points = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            Point point = new Point(in.readDouble(), in.readDouble());
            RequestPointType pointType = RequestPointType.valueOf(in.readString());
            String pointContext = in.readString();
            points.add(new RequestPoint(
                    point,
                    pointType,
                    pointContext));
        }
        return points;
    }

    private VehicleOptions readVehicleOptions(Parcel in) {
        VehicleOptions vOptions = new VehicleOptions();
        vOptions.setVehicleType(VehicleType.valueOf(in.readString()));
        vOptions.setWeight((Float)in.readValue(null));
        vOptions.setAxleWeight((Float)in.readValue(null));
        vOptions.setMaxWeight((Float)in.readValue(null));
        vOptions.setHeight((Float)in.readValue(null));
        vOptions.setWidth((Float)in.readValue(null));
        vOptions.setLength((Float)in.readValue(null));
        vOptions.setPayload((Float)in.readValue(null));
        vOptions.setEcoClass(in.readInt());
        vOptions.setHasTrailer(in.readInt() == 1);
        return vOptions;
    }

    private void writeVehicleOptions(Parcel out, VehicleOptions vehicleOptions) {
        out.writeString(vehicleOptions.getVehicleType() == null ? VehicleType.DEFAULT.name() : vehicleOptions.getVehicleType().name());
        out.writeValue(vehicleOptions.getWeight());
        out.writeValue(vehicleOptions.getAxleWeight());
        out.writeValue(vehicleOptions.getMaxWeight());
        out.writeValue(vehicleOptions.getHeight());
        out.writeValue(vehicleOptions.getWidth());
        out.writeValue(vehicleOptions.getLength());
        out.writeValue(vehicleOptions.getPayload());
        out.writeInt((vehicleOptions.getEcoClass() == null) ? 6 : vehicleOptions.getEcoClass());
        out.writeInt(vehicleOptions.getHasTrailer() == null ? 0 : (vehicleOptions.getHasTrailer() ? 1 : 0));
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        writePointsArray(out, routePoints);
        writePointsArray(out, simulationRoutePoints);
        out.writeString(routeUri);
        out.writeString(simulationRouteUri);
        out.writeString(selectorType.name());
        writeVehicleOptions(out, vehicleOptions);
        out.writeInt(useParkingRoutes ? 1 : 0);
        out.writeInt(disableAlternatives ? 1 : 0);
        out.writeInt(freeDrive ? 1 : 0);
    }

    public static final Parcelable.Creator<TestCase> CREATOR
            = new Parcelable.Creator<TestCase>() {
        public TestCase createFromParcel(Parcel in) {
            return new TestCase(in);
        }

        public TestCase[] newArray(int size) {
            return new TestCase[size];
        }
    };

    private TestCase(Parcel in) {
        routePoints = readPointsArray(in);
        simulationRoutePoints = readPointsArray(in);
        routeUri = in.readString();
        if (routeUri.isEmpty())
            routeUri = null;
        simulationRouteUri = in.readString();
        if (simulationRouteUri.isEmpty())
            simulationRouteUri = null;
        selectorType = RouteSelector.SelectorType.valueOf(in.readString());
        vehicleOptions = readVehicleOptions(in);
        useParkingRoutes = in.readInt() != 0;
        disableAlternatives = in.readInt() != 0;
        freeDrive = in.readInt() != 0;
    }
}
