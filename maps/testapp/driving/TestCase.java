package com.yandex.maps.testapp.driving;

import android.os.Parcel;
import android.os.Parcelable;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TestCase implements Parcelable {
    private final String title;
    private final String description;
    private final List<RequestPoint> routePoints;

    public TestCase(String title, String description, RequestPoint[] routePoints) {
        this.title = title;
        this.description = description;
        this.routePoints = Arrays.asList(routePoints);
    }

    protected TestCase(Parcel in) {
        title = in.readString();
        description = in.readString();
        routePoints = readPoints(in);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<RequestPoint> getRoutePoints() {
        return routePoints;
    }

    public static final Creator<TestCase> CREATOR = new Creator<TestCase>() {
        @Override
        public TestCase createFromParcel(Parcel in) {
            return new TestCase(in);
        }

        @Override
        public TestCase[] newArray(int size) {
            return new TestCase[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(title);
        out.writeString(description);
        writePoints(out, routePoints);
    }

    private void writePoints(Parcel out, List<RequestPoint> routePoints) {
        out.writeInt(routePoints.size());
        for (RequestPoint point: routePoints) {
            out.writeDouble(point.getPoint().getLatitude());
            out.writeDouble(point.getPoint().getLongitude());
            out.writeString(point.getType().toString());
            out.writeString(point.getPointContext());
        }
    }

    private List<RequestPoint> readPoints(Parcel in) {
        int size = in.readInt();
        List<RequestPoint> routePoints = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            Point point = new Point(in.readDouble(), in.readDouble());
            RequestPointType pointType = RequestPointType.valueOf(in.readString());
            String context = in.readString();
            routePoints.add(new RequestPoint(point, pointType, context));
        }
        return routePoints;
    }
}
