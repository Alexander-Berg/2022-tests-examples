package ru.yandex.solomon.math.operation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.solomon.math.protobuf.Aggregation;
import ru.yandex.solomon.math.protobuf.Operation;
import ru.yandex.solomon.math.protobuf.OperationAggregationSummary;
import ru.yandex.solomon.math.protobuf.OperationCast;
import ru.yandex.solomon.math.protobuf.OperationCombine;
import ru.yandex.solomon.math.protobuf.OperationDownsampling;
import ru.yandex.solomon.math.protobuf.OperationDropTimeSeries;
import ru.yandex.solomon.math.protobuf.OperationTop;
import ru.yandex.solomon.model.protobuf.MetricType;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class OperationsSplitTest {

    @Test
    public void empty() {
        SplittedOperations result = Operations.splitOperations(Collections.emptyList());
        assertThat(result.getClientOps(), emptyIterable());
        assertThat(result.getServerOps(), emptyIterable());
    }

    @Test
    public void downsampling() {
        List<Operation> source = ImmutableList.of(opDownsampling());

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), emptyIterable());
        assertThat(result.getServerOps(), equalTo(source));
    }

    @Test
    public void summary() {
        List<Operation> source = ImmutableList.of(opSummary(Aggregation.AVG));

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), emptyIterable());
        assertThat(result.getServerOps(), equalTo(source));
    }

    @Test
    public void summary_dropTs() {
        List<Operation> source = ImmutableList.of(opSummary(Aggregation.AVG), opDropTs());

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), emptyIterable());
        assertThat(result.getServerOps(), equalTo(source));
    }

    @Test
    public void downsampling_summary_dropTs() {
        List<Operation> source = ImmutableList.of(opDownsampling(), opSummary(Aggregation.SUM), opDropTs());

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), emptyIterable());
        assertThat(result.getServerOps(), equalTo(source));
    }

    @Test
    public void top() {
        List<Operation> source = ImmutableList.of(opTop(Aggregation.AVG));

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), equalTo(source));
        assertThat(result.getServerOps(), equalTo(source));
    }

    @Test
    public void downsampling_top() {
        List<Operation> source = ImmutableList.of(opDownsampling(), opTop(Aggregation.AVG));

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), equalTo(ImmutableList.of(opTop(Aggregation.AVG))));
        assertThat(result.getServerOps(), equalTo(ImmutableList.of(opDownsampling(), opTop(Aggregation.AVG))));
    }

    @Test
    public void top_downsampling() {
        List<Operation> source = ImmutableList.of(opTop(Aggregation.MAX), opDownsampling());

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), equalTo(ImmutableList.of(opTop(Aggregation.MAX), opDownsampling())));
        assertThat(result.getServerOps(), equalTo(ImmutableList.of(opTop(Aggregation.MAX))));
    }

    @Test
    public void combineSum() {
        List<Operation> source = ImmutableList.of(opCombine(Aggregation.SUM));

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), equalTo(ImmutableList.of(opCombine(Aggregation.SUM))));
        assertThat(result.getServerOps(), equalTo(ImmutableList.of(opCombine(Aggregation.SUM))));
    }

    @Test
    public void downsampling_combineSum() {
        List<Operation> source = ImmutableList.of(opDownsampling(), opCombine(Aggregation.SUM));

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), equalTo(ImmutableList.of(opCombine(Aggregation.SUM))));
        assertThat(result.getServerOps(), equalTo(ImmutableList.of(opDownsampling(), opCombine(Aggregation.SUM))));
    }

    @Test
    public void combineSum_downsampling() {
        List<Operation> source = ImmutableList.of(opCombine(Aggregation.SUM), opDownsampling());

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), equalTo(ImmutableList.of(opCombine(Aggregation.SUM), opDownsampling())));
        assertThat(result.getServerOps(), equalTo(ImmutableList.of(opCombine(Aggregation.SUM))));
    }

    @Test
    public void downsampling_top_combineSum() {
        List<Operation> source = ImmutableList.of(opDownsampling(), opTop(Aggregation.AVG), opCombine(Aggregation.SUM));

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), equalTo(ImmutableList.of(opTop(Aggregation.AVG), opCombine(Aggregation.SUM))));
        assertThat(result.getServerOps(), equalTo(ImmutableList.of(opDownsampling(), opTop(Aggregation.AVG))));
    }

    @Test
    public void combineAvg() {
        List<Operation> source = ImmutableList.of(opCombine(Aggregation.AVG));

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), equalTo(ImmutableList.of(opCombine(Aggregation.AVG))));
        assertThat(result.getServerOps(), equalTo(ImmutableList.of()));
    }

    @Test
    public void downsampling_top_summary_dropTs() {
        List<Operation> source = ImmutableList.of(opDownsampling(), opTop(Aggregation.AVG), opSummary(Aggregation.SUM), opDropTs());

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), equalTo(ImmutableList.of(opTop(Aggregation.AVG), opSummary(Aggregation.SUM), opDropTs())));
        assertThat(result.getServerOps(), equalTo(ImmutableList.of(opDownsampling(), opTop(Aggregation.AVG))));
    }

    @Test
    public void cast() {
        List<Operation> source = ImmutableList.of(opCast(MetricType.IGAUGE));

        SplittedOperations result = Operations.splitOperations(source);
        assertThat(result.getClientOps(), emptyIterable());
        assertThat(result.getServerOps(), equalTo(source));
    }

    private Operation opDownsampling() {
        return Operation.newBuilder()
                .setDownsampling(OperationDownsampling.newBuilder()
                        .setAggregation(Aggregation.AVG)
                        .setGridMillis(180_000)
                        .setFillOption(OperationDownsampling.FillOption.PREVIOUS)
                        .build())
                .build();
    }

    private Operation opSummary(Aggregation... opts) {
        return Operation.newBuilder()
                .setSummary(OperationAggregationSummary.newBuilder()
                        .addAllAggregations(Arrays.asList(opts))
                        .build())
                .build();
    }

    private Operation opDropTs() {
        return Operation.newBuilder()
                .setDropTimeseries(OperationDropTimeSeries.newBuilder().build())
                .build();
    }

    private Operation opTop(Aggregation aggregation) {
        return Operation.newBuilder()
                .setTop(OperationTop.newBuilder()
                        .setLimit(3)
                        .setTimeAggregation(aggregation)
                        .build())
                .build();
    }

    private Operation opCombine(Aggregation aggregation) {
        return Operation.newBuilder()
                .setCombine(OperationCombine.newBuilder()
                        .setAggregation(aggregation)
                        .build())
                .build();
    }

    private Operation opCast(MetricType type) {
        return Operation.newBuilder()
                .setCast(OperationCast.newBuilder()
                        .setType(type)
                        .build())
                .build();
    }

}
