package com.yandex.maps.testapp.common_routing;

import android.content.Context;

import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.BoundingBoxHelper;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.Subpolyline;
import com.yandex.mapkit.geometry.SubpolylineHelper;
import com.yandex.mapkit.transport.masstransit.ConstructionSegment;
import com.yandex.mapkit.transport.masstransit.Route;
import com.yandex.mapkit.transport.masstransit.RouteStop;
import com.yandex.mapkit.transport.masstransit.RouteStopMetadata;
import com.yandex.mapkit.transport.masstransit.Section;
import com.yandex.mapkit.transport.masstransit.SectionMetadata;
import com.yandex.mapkit.transport.masstransit.RestrictedEntry;
import com.yandex.mapkit.transport.masstransit.Annotation;
import com.yandex.maps.testapp.R;

import java.util.ArrayList;
import java.util.List;

public class AnnotatedSection {
    private final String annotation;
    private final String alertsText;
    private final Polyline routePolyline;
    private Point point;
    private final Subpolyline subpolyline;
    private List<Integer> constructions;
    private List<RestrictedEntry> restrictedEntries;
    private List<Annotation> walkAnnotations;

    public Polyline getPolyline() {
        return SubpolylineHelper.subpolyline(routePolyline, subpolyline);
    }

    public Point getPoint() {
        return point;
    }

    public Subpolyline getSubpolyline() {
        return subpolyline;
    }

    public BoundingBox getBoundingBox() {
        return BoundingBoxHelper.getBounds(getPolyline());
    }

    public String getAnnotation() {
        return annotation;
    }

    public String getAlertsText() { return alertsText; }

    public List<Integer> getConstructions() {
        return constructions;
    }

    public List<RestrictedEntry> getRestrictedEntries() { return restrictedEntries; }

    public AnnotatedSection(Section section, Route route, Context ctx) {
        StringBuilder builder = new StringBuilder();
        builder.append(Helpers.formatMetadata(section.getMetadata(), ctx));
        for(RouteStop stop: section.getStops()) {
            builder.append("\n");
            RouteStopMetadata stopMeta = stop.getMetadata();
            builder.append(String.format(
                ctx.getString(R.string.masstransit_stop), stopMeta.getStop().getName()));
            if (stopMeta.getStopExit() != null) {
                builder.append(" ");
                builder.append(String.format(
                    ctx.getString(R.string.masstransit_stop_exit), stopMeta.getStopExit().getName()));
                if (stopMeta.getExitPoint() != null) {
                    Point point = stopMeta.getExitPoint();
                    builder.append(" ");
                    builder.append(String.format(
                            ctx.getString(R.string.masstransit_at_point), point.getLongitude(), point.getLatitude()));
                }
            }
        }
        this.annotation = builder.toString();
        this.routePolyline = route.getGeometry();
        this.subpolyline = section.getGeometry();
        this.restrictedEntries = new ArrayList<>();
        this.constructions = new ArrayList<>();
        this.walkAnnotations = new ArrayList<>();

        final SectionMetadata.SectionData data = section.getMetadata().getData();

        if (data.getWait() != null) {
            this.alertsText = null;
            this.point = getPolyline().getPoints().get(0);
            return;
        }

        this.alertsText = Helpers.makeAlertsText(section, ctx);
        final List<ConstructionSegment> constructionSegments;
        if (data.getFitness() != null) {
            constructionSegments = data.getFitness().getConstructions();
            this.restrictedEntries = data.getFitness().getRestrictedEntries();
            this.walkAnnotations = data.getFitness().getAnnotations();
        } else if (data.getTransfer() != null) {
            constructionSegments = data.getTransfer().getConstructions();
        } else {
            return;
        }
        this.constructions = Helpers.getFlatArray(
            constructionSegments.size(), null,
            new ConstructionResolver() {
                @Override
                public Subpolyline subpolyline(int index) {
                    return constructionSegments.get(index).getSubpolyline();
                }

                @Override
                public Integer id(int index) {
                    return constructionSegments.get(index).getConstruction().ordinal();
                }
            });

    }

    public List<Annotation> getAnnotations() {
        return walkAnnotations;
    }
}
