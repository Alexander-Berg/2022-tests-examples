package ru.yandex.stockpile.client;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.model.protobuf.MetricId;
import ru.yandex.solomon.model.protobuf.MetricType;
import ru.yandex.stockpile.api.CreateMetricRequest;
import ru.yandex.stockpile.api.CreateMetricResponse;
import ru.yandex.stockpile.api.EStockpileStatusCode;
import ru.yandex.stockpile.api.TCompressedReadResponse;
import ru.yandex.stockpile.api.TCompressedWriteRequest;
import ru.yandex.stockpile.api.TCompressedWriteResponse;
import ru.yandex.stockpile.api.TPoint;
import ru.yandex.stockpile.api.TReadRequest;
import ru.yandex.stockpile.api.TReadResponse;
import ru.yandex.stockpile.api.TWriteRequest;
import ru.yandex.stockpile.api.TWriteResponse;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public final class TestUtil {
    private TestUtil() {
    }

    public static long timeToMillis(String time) {
        return Instant.parse(time).toEpochMilli();
    }

    public static MetricId metricId(int shardId, long localId) {
        return MetricId.newBuilder()
                .setShardId(shardId)
                .setLocalId(localId)
                .build();
    }

    public static TPoint point(String time, double value) {
        return TPoint.newBuilder()
                .setTimestampsMillis(timeToMillis(time))
                .setDoubleValue(value)
                .build();
    }

    static TReadResponse syncReadOne(StockpileClient client, TReadRequest request) {
        TReadResponse response = client.readOne(request).join();

        if (response.getStatus() != EStockpileStatusCode.OK) {
            throw new IllegalStateException(response.getStatus() + ": " + response.getStatusMessage());
        }

        return response;
    }

    static TCompressedReadResponse syncCompressedReadOne(StockpileClient client, TReadRequest request) {
        TCompressedReadResponse response = client.readCompressedOne(request).join();

        if (response.getStatus() != EStockpileStatusCode.OK) {
            throw new IllegalStateException(response.getStatus() + ": " + response.getStatusMessage());
        }

        return response;

    }

    static void syncWriteOne(StockpileClient client, TWriteRequest request) {
        TWriteResponse response = client.writeOne(request).join();

        if (response.getStatus() != EStockpileStatusCode.OK) {
            throw new IllegalStateException(response.getStatus() + ": " + response.getStatusMessage());
        }
    }

    static void syncWriteCompressedOne(StockpileClient client, TCompressedWriteRequest request) {
        TCompressedWriteResponse response = client.writeCompressedOne(request).join();

        if (response.getStatus() != EStockpileStatusCode.OK) {
            throw new IllegalStateException(response.getStatus() + ": " + response.getStatusMessage());
        }
    }

    static MetricId createMetric(StockpileClient client, MetricType type) {
        return createMetric(client, type, MetricId.getDefaultInstance());
    }

    static MetricId createMetric(StockpileClient client, MetricType type, MetricId metricId) {
        CreateMetricRequest request = CreateMetricRequest.newBuilder()
                .setType(type)
                .setMetricId(metricId)
                .build();

        CreateMetricResponse response = client.createMetric(request).join();

        if (response.getStatus() != EStockpileStatusCode.OK) {
            throw new IllegalStateException(response.getStatus() + ": " + response.getStatusMessage());
        }

        return response.getMetricId();
    }

    static MetricId createMetric(StockpileClient client, MetricType type, int shardId) {
        return createMetric(client, type, MetricId.newBuilder()
            .setShardId(shardId)
            .build());
    }

    static List<TPoint> randomDoublePoints(MetricId metricId) {
        return Arrays.asList(
                TPoint.newBuilder()
                        .setTimestampsMillis(System.currentTimeMillis() - 3)
                        .setDoubleValue(metricId.getShardId())
                        .build(),

                TPoint.newBuilder()
                        .setTimestampsMillis(System.currentTimeMillis() - 2)
                        .setDoubleValue(metricId.getLocalId())
                        .build(),

                TPoint.newBuilder()
                        .setTimestampsMillis(System.currentTimeMillis() - 1)
                        .setDoubleValue(Math.random())
                        .build()
        );
    }
}
