package ru.yandex.solomon.expression.expr.func;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.expression.NamedGraphData;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Vladimir Gordiychuk
 */
public class LabelsUtilTest {

    @Test
    public void emptyCommonLabels() {
        Labels result = LabelsUtil.getCommonLabels(Collections.emptyList());
        assertThat(result.isEmpty(), equalTo(true));
    }

    @Test
    public void commonNamespace() {
        Labels result = LabelsUtil.getCommonLabels(Arrays.asList(
                NamedGraphData.of(Labels.of("sensor", "responseBytes", "host", "solomon-01")),
                NamedGraphData.of(Labels.of("sensor", "responseBytes", "host", "solomon-02"))
        ));

        assertThat(result, equalTo(Labels.of("sensor", "responseBytes")));
    }

    @Test
    public void commonDimension() {
        Labels result = LabelsUtil.getCommonLabels(Arrays.asList(
                NamedGraphData.of(Labels.of("sensor", "started", "host", "solomon-01")),
                NamedGraphData.of(Labels.of("sensor", "completed", "host", "solomon-01"))
        ));

        assertThat(result, equalTo(Labels.of("host", "solomon-01")));
    }

    @Test
    public void commonForDifferentLabelsSizes() {
        List<NamedGraphData> source = Arrays.asList(
            NamedGraphData.of(Labels.of("sensor", "started")),
            NamedGraphData.of(Labels.of("sensor", "started", "host", "solomon-01"))
        );

        assertThat(LabelsUtil.getCommonLabels(source), equalTo(Labels.of("sensor", "started")));

        Collections.reverse(source);
        assertThat(LabelsUtil.getCommonLabels(source), equalTo(Labels.of("sensor", "started")));
    }

    @Test
    public void commonAbsent() {
        Labels result = LabelsUtil.getCommonLabels(Arrays.asList(
                NamedGraphData.of(Labels.of("sensor", "started")),
                NamedGraphData.of(Labels.of("sensor", "completed"))
        ));

        assertThat(result, equalTo(Labels.empty()));
    }

    @Test
    public void commonLabelsForAbsentLabels() {
        Labels result = LabelsUtil.getCommonLabels(Arrays.asList(
                NamedGraphData.of(Labels.empty()),
                NamedGraphData.of(Labels.empty())
        ));

        assertThat(result, equalTo(Labels.empty()));
    }
}
