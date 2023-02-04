package ru.yandex.webmaster3.viewer.searchquery;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.searchquery.QueryId;
import ru.yandex.webmaster3.core.searchquery.QueryIndicator;
import ru.yandex.webmaster3.storage.searchquery.QueryStat;
import ru.yandex.webmaster3.storage.searchquery.util.Accumulator;
import ru.yandex.webmaster3.storage.searchquery.util.ExtractorFactory;

/**
 * @author aherman
 */
public class ExtractorFactoryTest {
    @Test
    public void testTotalShowsCount() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.TOTAL_SHOWS_COUNT);
        updateAccumulator(accumulator);
        Assert.assertEquals(14, accumulator.getValue(), 0.01);
    }

    @Test
    public void testTotalClicksCount() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.TOTAL_CLICKS_COUNT);
        updateAccumulator(accumulator);
        Assert.assertEquals(10, accumulator.getValue(), 0.01);
    }

    @Test
    public void testTotalCtr() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.TOTAL_CTR);
        updateAccumulator(accumulator);
        Assert.assertEquals(10 / 14.0, accumulator.getValue(), 0.01);
    }

    @Test
    public void testAvgShowPos() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.AVERAGE_SHOW_POSITION);
        updateAccumulator(accumulator);
        Assert.assertEquals(1.0 * (5 * 1 + 4 * 2 + 3 * 4 + 2 * 11) / (5 + 4 + 3 + 2), accumulator.getValue(), 0.01);
    }

    @Test
    public void testAvgClickPos() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.AVERAGE_CLICK_POSITION);
        updateAccumulator(accumulator);
        Assert.assertEquals(1.0 * (4 * 1 + 3 * 3 + 2 * 5 + 1 * 12) / (4 + 3 + 2 + 1)  , accumulator.getValue(), 0.01);
    }

    @Test
    public void testShows_1() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.SHOWS_COUNT_1);
        updateAccumulator(accumulator);
        Assert.assertEquals(5, accumulator.getValue(), 0.01);
    }

    @Test
    public void testClicks_1() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.CLICKS_COUNT_1);
        updateAccumulator(accumulator);
        Assert.assertEquals(4, accumulator.getValue(), 0.01);
    }

    @Test
    public void testCtr_1() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.CTR_1);
        updateAccumulator(accumulator);
        Assert.assertEquals(4.0 / 5.0, accumulator.getValue(), 0.01);
    }

    @Test
    public void testShows_2_3() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.SHOWS_COUNT_2_3);
        updateAccumulator(accumulator);
        Assert.assertEquals(4, accumulator.getValue(), 0.01);
    }

    @Test
    public void testClicks_2_3() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.CLICKS_COUNT_2_3);
        updateAccumulator(accumulator);
        Assert.assertEquals(3, accumulator.getValue(), 0.01);
    }

    @Test
    public void testCtr_2_3() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.CTR_2_3);
        updateAccumulator(accumulator);
        Assert.assertEquals(3.0 / 4.0, accumulator.getValue(), 0.01);
    }

    @Test
    public void testAvgShowPos_2_3() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.AVERAGE_SHOW_POSITION_2_3);
        updateAccumulator(accumulator);
        Assert.assertEquals(2, accumulator.getValue(), 0.01);
    }

    @Test
    public void testAvgClickPos_2_3() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.AVERAGE_CLICK_POSITION_2_3);
        updateAccumulator(accumulator);
        Assert.assertEquals(3, accumulator.getValue(), 0.01);
    }

    @Test
    public void testShows_4_10() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.SHOWS_COUNT_4_10);
        updateAccumulator(accumulator);
        Assert.assertEquals(3, accumulator.getValue(), 0.01);
    }

    @Test
    public void testClicks_4_10() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.CLICKS_COUNT_4_10);
        updateAccumulator(accumulator);
        Assert.assertEquals(2, accumulator.getValue(), 0.01);
    }

    @Test
    public void testCtr_4_10() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.CTR_4_10);
        updateAccumulator(accumulator);
        Assert.assertEquals(2.0 / 3.0, accumulator.getValue(), 0.01);
    }

    @Test
    public void testAvgShowPos_4_10() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.AVERAGE_SHOW_POSITION_4_10);
        updateAccumulator(accumulator);
        Assert.assertEquals(4, accumulator.getValue(), 0.01);
    }

    @Test
    public void testAvgClickPos_4_10() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.AVERAGE_CLICK_POSITION_4_10);
        updateAccumulator(accumulator);
        Assert.assertEquals(5, accumulator.getValue(), 0.01);
    }

    @Test
    public void testShows_11_50() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.SHOWS_COUNT_11_50);
        updateAccumulator(accumulator);
        Assert.assertEquals(2, accumulator.getValue(), 0.01);
    }

    @Test
    public void testClicks_11_50() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.CLICKS_COUNT_11_50);
        updateAccumulator(accumulator);
        Assert.assertEquals(1, accumulator.getValue(), 0.01);
    }

    @Test
    public void testCtr_11_50() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.CTR_11_50);
        updateAccumulator(accumulator);
        Assert.assertEquals(1.0 / 2.0, accumulator.getValue(), 0.01);
    }

    @Test
    public void testAvgShowPos_11_50() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.AVERAGE_SHOW_POSITION_11_50);
        updateAccumulator(accumulator);
        Assert.assertEquals(11, accumulator.getValue(), 0.01);
    }

    @Test
    public void testAvgClickPos_11_50() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.AVERAGE_CLICK_POSITION_11_50);
        updateAccumulator(accumulator);
        Assert.assertEquals(12, accumulator.getValue(), 0.01);
    }

    @Test
    public void testShows_1_50() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.SHOWS_1_50);
        updateAccumulator(accumulator);
        Assert.assertEquals(14, accumulator.getValue(), 0.01);
    }

    @Test
    public void testClicks_1_50() throws Exception {
        Accumulator accumulator = ExtractorFactory.createExtractor(QueryIndicator.CLICKS_1_50);
        updateAccumulator(accumulator);
        Assert.assertEquals(10, accumulator.getValue(), 0.01);
    }

    private void updateAccumulator(Accumulator accumulator) {
        QueryId queryId = new QueryId(1);
        accumulator.apply(new QueryStat(new LocalDate("2016-01-01"), queryId, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        accumulator.apply(new QueryStat(new LocalDate("2016-01-01"), queryId, 2, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1 * 2, 0, 0, 0, 0, 0, 0));
        accumulator.apply(new QueryStat(new LocalDate("2016-01-01"), queryId, 3, 2, 1, 1, 1, 1, 1, 0, 0, 0, 1 * 2, 1 * 3, 1 * 4, 0, 0, 0, 0));
        accumulator.apply(new QueryStat(new LocalDate("2016-01-01"), queryId, 4, 3, 1, 1, 1, 1, 1, 1, 1, 0, 1 * 2, 1 * 3, 1 * 4, 1 * 5, 1 * 11, 0, 0));
        accumulator.apply(new QueryStat(new LocalDate("2016-01-01"), queryId, 4, 4, 1, 1, 1, 1, 1, 1, 1, 1, 1 * 2, 1 * 3, 1 * 4, 1 * 5, 1 * 11, 1 * 12, 0));
    }
}
