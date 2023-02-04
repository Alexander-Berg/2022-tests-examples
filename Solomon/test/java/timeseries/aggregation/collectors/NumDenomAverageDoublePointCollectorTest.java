package ru.yandex.solomon.model.timeseries.aggregation.collectors;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.model.point.AggrPoint;
import ru.yandex.solomon.model.point.AggrPointDataTestSupport;
import ru.yandex.solomon.model.point.column.StockpileColumn;
import ru.yandex.solomon.model.point.column.ValueColumn;
import ru.yandex.solomon.model.point.column.ValueObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class NumDenomAverageDoublePointCollectorTest {

    private PointValueCollector collector;

    @Before
    public void setUp() throws Exception {
        collector = new NumDenomAverageDoublePointCollector();
    }

    @Test
    public void emptyCompute() {
        AggrPoint point = new AggrPoint(StockpileColumn.VALUE.mask());
        assertFalse(collector.compute(point));
        assertTrue(Double.isNaN(point.valueNum));
    }

    @Test
    public void onePointCompute() {
        int mask = StockpileColumn.VALUE.mask();
        AggrPoint source = AggrPointDataTestSupport.randomPoint(mask);
        collector.append(source);
        AggrPoint result = new AggrPoint(mask);
        assertTrue(collector.compute(result));
        assertEquals(source, result);
    }

    @Test
    public void computeAbsentDenom() {
        assertEquals(vo(20), compute(vo(20)));
        assertEquals(vo(30), compute(vo(20), vo(40)));
        assertEquals(vo(50), compute(vo(20), vo(40), vo(90)));
    }

    @Test
    public void sumAvgSameDenom() {
        assertEquals(vo(90, 30_000), compute(vo(30, 15_000), vo(60, 15_000)));
    }

    @Test
    public void sumAvgMixedWithNonZeroDenom() {
        assertEquals(vo(3), compute(vo(30, 15_000), vo(120, 30_000)));
    }

    @Test
    public void sumAvgMixedWithZeroDenom() {
        assertEquals(vo(3), compute(vo(30, 15_000), vo(4)));
    }

    @Test
    public void complexCase() {
        compute(vo(3), compute(vo(30, 15_000), vo(60, 15_000), vo(3)));
    }

    private ValueObject vo(double num) {
        return vo(num, ValueColumn.DEFAULT_DENOM);
    }

    private ValueObject vo(double num, long denom) {
        return new ValueObject(num, denom);
    }

    private ValueObject compute(ValueObject... source) {
        return compute(Arrays.asList(source));
    }

    private ValueObject compute(List<ValueObject> source) {
        collector.reset();
        AggrPoint temp = new AggrPoint(StockpileColumn.VALUE.mask());
        for (ValueObject vo : source) {
            temp.setValue(vo.num, vo.denom);
            collector.append(temp);
        }

        temp.clearFields(StockpileColumn.VALUE.mask());
        assertTrue(collector.compute(temp));
        return vo(temp.valueNum, temp.valueDenom);
    }
}
