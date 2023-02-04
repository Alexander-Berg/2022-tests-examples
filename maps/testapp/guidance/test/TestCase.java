package com.yandex.maps.testapp.guidance.test;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.RouteSerializer;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.driving.VehicleType;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.List;

public class TestCase implements Parcelable {
    public DrivingRoute route;
    public DrivingRoute simulationRoute;
    public List<RequestPoint> routePoints;
    public VehicleOptions vehicleOptions;
    public boolean useParkingRoutes;
    public boolean disableAlternatives;

    TestCase(DrivingRoute route,
             List<RequestPoint> routePoints,
             DrivingRoute simulationRoute,
             VehicleOptions vehicleOptions,
             boolean useParkingRoutes,
             boolean disableAlternatives) {
        this.route = route;
        this.simulationRoute = simulationRoute;
        this.routePoints = routePoints;
        this.vehicleOptions = vehicleOptions;
        this.useParkingRoutes = useParkingRoutes;
        this.disableAlternatives = disableAlternatives;
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
        }
    }

    private List<RequestPoint> readPointsArray(Parcel in) {
        int size = in.readInt();
        ArrayList<RequestPoint> points = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            Point point = new Point(in.readDouble(), in.readDouble());
            points.add(new RequestPoint(
                    point,
                    RequestPointType.WAYPOINT,
                    null /* pointContext */));
        }
        return points;
    }

    private void writeRoute(Parcel out, DrivingRoute route, RouteSerializer serializer) {
        if (route == null) {
            out.writeInt(0);
            return;
        }
        byte[] bytes = serializer.save(route);
        out.writeInt(bytes.length);
        out.writeByteArray(bytes);
    }

    private DrivingRoute readRoute(Parcel in, RouteSerializer serializer) {
        int size = in.readInt();
        if (size == 0) {
            return null;
        }
        byte[] bytes = new byte[size];
        in.readByteArray(bytes);
        return serializer.load(bytes);
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
        DrivingRouter router = DirectionsFactory.getInstance().createDrivingRouter();
        RouteSerializer serializer = router.routeSerializer();
        writeRoute(out, route, serializer);
        writeRoute(out, simulationRoute, serializer);
        writePointsArray(out, routePoints);
        writeVehicleOptions(out, vehicleOptions);
        out.writeInt(useParkingRoutes ? 1 : 0);
        out.writeInt(disableAlternatives ? 1 : 0);
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
        DrivingRouter router = DirectionsFactory.getInstance().createDrivingRouter();
        RouteSerializer serializer = router.routeSerializer();
        route = readRoute(in, serializer);
        simulationRoute = readRoute(in, serializer);
        routePoints = readPointsArray(in);
        vehicleOptions = readVehicleOptions(in);
        useParkingRoutes = in.readInt() != 0;
        disableAlternatives = in.readInt() != 0;
    }
}

