package ru.yandex.navi;

public class RoutePoint {
    public enum Type {
        VIA,
        WAY
    }

    final Type type;
    final GeoPoint point;

    private RoutePoint(Type type, GeoPoint point) {
        this.type = type;
        this.point = point;
    }

    public static RoutePoint via(GeoPoint point) {
        return new RoutePoint(Type.VIA, point);
    }

    public static RoutePoint way(GeoPoint point) {
        return new RoutePoint(Type.WAY, point);
    }
}
