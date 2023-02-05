package ru.yandex.disk.feed;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ImageGridConfigProviderTabletTest extends ImageGridConfigProviderTest {

    @Override
    public void before() throws Exception {
        gridConfigProvider = new ImageGridConfigProvider();
        isTablet = true;
    }

    @Test
    public void testItemHeight() {
        assertEquals(380, gridConfigProvider.getItemHeight(2, 110, 1080, isTablet));
        assertEquals(380, gridConfigProvider.getItemHeight(2, 150, 1080, isTablet));
        assertEquals(190, gridConfigProvider.getItemHeight(1, 120, 1080, isTablet));
        assertEquals(190, gridConfigProvider.getItemHeight(1, 121, 1080, isTablet));
        assertEquals(760, gridConfigProvider.getItemHeight(2, 60, 1080, isTablet));
        assertEquals(760, gridConfigProvider.getItemHeight(2, 80, 1080, isTablet));
        assertEquals(380, gridConfigProvider.getItemHeight(1, 20, 1080, isTablet));
        assertEquals(380, gridConfigProvider.getItemHeight(1, 90, 1080, isTablet));

        assertEquals(1520, gridConfigProvider.getItemHeight(4, 90, 1080, isTablet));
        assertEquals(1520, gridConfigProvider.getItemHeight(4, 70, 1080, isTablet));
        assertEquals(760, gridConfigProvider.getItemHeight(4, 110, 1080, isTablet));
        assertEquals(760, gridConfigProvider.getItemHeight(4, 120, 1080, isTablet));
    }

    @Override
    protected void testFourLandSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, i < 4 ? (i == 0) ? 2 : 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Override
    protected void testTwoLandOnePortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 3) ? (i == 1) ? 2 : 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Override
    protected void testThreePortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 3) ? 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Override
    protected void testOnePortOneLandSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 2) ? (i == 0) ? 2 : 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Override
    protected void testOnePortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, i == 0 ? 4 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Override
    protected void testOneLandSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, i == 0 ? 4 : -1);
        }
        testSpan(config, expectedSpans);
    }
}
