package com.yandex.maps.testapp.common_routing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.yandex.mapkit.geometry.Subpolyline;
import com.yandex.mapkit.transport.masstransit.Alert;
import com.yandex.mapkit.transport.masstransit.BoardingOptions;
import com.yandex.mapkit.transport.masstransit.Line;
import com.yandex.mapkit.transport.masstransit.RouteMetadata;
import com.yandex.mapkit.transport.masstransit.RouteSettings;
import com.yandex.mapkit.transport.masstransit.Section;
import com.yandex.mapkit.transport.masstransit.SectionMetadata;
import com.yandex.mapkit.transport.masstransit.Stop;
import com.yandex.mapkit.transport.masstransit.Transport;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.image.ImageProvider;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Helpers {

    static String makeAlertsText(@NotNull Section section, Context ctx) {
        final SectionMetadata metadata = section.getMetadata();
        boolean hasAlerts = false;
        if (metadata.getData().getTransports() == null)
            return null;

        StringBuilder builder = new StringBuilder();
        builder.append(metadata.getWeight().getTime().getText());
        builder.append(": ");
        builder.append(ctx.getString(R.string.masstransit_ride));

        for (Transport transport: metadata.getData().getTransports()) {
            Line line = transport.getLine();
            builder.append("\n\n");
            builder.append(line.getName());
            builder.append("( ");
            if (line.getIsNight()) {
                builder.append(ctx.getString(R.string.masstransit_night));
                builder.append(" ");
            }

            for (String vehicleType: line.getVehicleTypes()) {
                builder.append(vehicleType);
                builder.append(" ");
            }
            builder.append(")\n");

            for (Transport.TransportThread thread : transport.getTransports()) {
                Stop stop = section.getStops().get(0).getMetadata().getStop();
                if (thread.getAlternateDepartureStop() != null) {
                    stop = thread.getAlternateDepartureStop();
                }
                if (thread.getAlerts() != null) {
                    for (Alert alert : thread.getAlerts()) {
                        builder.append("Thread ").append(thread.getThread().getId()).append(": ");
                        if (alert.getClosed() != null) {
                            builder.append("[Alert.Closed]");
                        }
                        if (alert.getClosedUntil() != null) {
                            builder.append("[Alert.ClosedUntil, time=");
                            builder.append(alert.getClosedUntil().getTime().getText());
                            builder.append(", stop=").append(stop.getName()).append("]");
                        }
                        if (alert.getLastTrip() != null) {
                            builder.append("[Alert.LastTrip, time=");
                            builder.append(alert.getLastTrip().getTime().getText());
                            builder.append(", stop=").append(stop.getName()).append("]");
                        }
                        builder.append(" ").append(alert.getText());
                        hasAlerts = true;
                    }
                }
            }
        }
        if (hasAlerts)
            return builder.toString();
        else
            return null;
    }

    public static String formatMetadata(SectionMetadata metadata, Context ctx) {
        if (metadata.getData().getWait() != null) {
            return String.format(
                ctx.getString(R.string.masstransit_wait),
                metadata.getWeight().getTime().getText());
        } else if (metadata.getData().getFitness() != null) {
            return String.format(
                ctx.getString(R.string.masstransit_walk),
                metadata.getWeight().getWalkingDistance().getText(),
                metadata.getWeight().getTime().getText());
        } else if (metadata.getData().getTransfer() != null) {
            return ctx.getString(R.string.masstransit_transfer);
        } else {
            StringBuilder builder = new StringBuilder();
            if (metadata.getEstimation() != null) {
                builder.append(metadata.getEstimation().getDepartureTime().getText()).append(" — ")
                    .append(metadata.getEstimation().getArrivalTime().getText()).append("\n");
            }
            builder.append(metadata.getWeight().getTime().getText()).append(": ");
            builder.append(ctx.getString(R.string.masstransit_ride));

            for (Transport transport: metadata.getData().getTransports()) {
                Line line = transport.getLine();
                builder.append("\n").append(line.getName()).append("( ");
                if (line.getIsNight()) {
                    builder.append(ctx.getString(R.string.masstransit_night)).append(" ");
                }

                for (String vehicleType: line.getVehicleTypes()) {
                    builder.append(vehicleType).append(" ");
                }
                builder.append(")");

                for (Transport.TransportThread thread : transport.getTransports()) {
                    if (thread.getAlerts() != null) {
                        for (Alert alert : thread.getAlerts()) {
                            builder.append(" ").append(alert.getText());
                        }
                    }
                    if (thread.getBoardingOptions() != null) {
                        List<BoardingOptions.BoardingArea> area = thread.getBoardingOptions().getArea();
                        if (area != null && !area.isEmpty()) {
                            List<String> boardings = new ArrayList<String>();
                            for (BoardingOptions.BoardingArea boardingOption: area) {
                                String id = boardingOption.getId();
                                if (id != null && id.startsWith("train_car:")) {
                                    boardings.add(id.substring("train_car:".length()));
                                }
                            }
                            if (!boardings.isEmpty()) {
                                builder.append("\nBoarding options: ");
                                builder.append(String.join(", ", boardings));
                            }
                        }
                    }
                }
            }
            return builder.toString();
        }
    }

    public static String formatMetadata(RouteMetadata metadata, Context ctx) {
        String time = metadata.getWeight().getTime().getText();
        if (metadata.getEstimation() != null) {
            time += " Dep " + metadata.getEstimation().getDepartureTime().getText()
                + ", Arr " + metadata.getEstimation().getArrivalTime().getText();
        }
        String walkingDistance = metadata.getWeight().getWalkingDistance().getText();
        int transfers = metadata.getWeight().getTransfersCount();

        return String.format(ctx.getString(R.string.routing_masstransit_success),
            time,
            transfers == 0 ?
                ctx.getString(R.string.no_transfers) :
                ctx.getResources().getQuantityString(
                    R.plurals.transfers_number, transfers, transfers),
            walkingDistance);
    }

    public static ArrayList<Integer> getFlatArray(
            int size, Subpolyline leg, ConstructionResolver getter) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            int curLen = getter.subpolyline(i).getEnd().getSegmentIndex() -
                getter.subpolyline(i).getBegin().getSegmentIndex() + 1;
            result.addAll(Collections.nCopies(curLen, getter.id(i)));
        }
        if (leg != null) {
            return new ArrayList<>(result.subList(
                    leg.getBegin().getSegmentIndex(),
                    leg.getEnd().getSegmentIndex() + 1
            ));
        }
        return result;
    }
}
