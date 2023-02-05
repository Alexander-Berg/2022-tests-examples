package ru.yandex.disk.feed;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.disk.test.TestCase2;

public class ImageGridConfigProviderTest extends TestCase2 {

    protected ImageGridConfigProvider gridConfigProvider;

    protected boolean isTablet;

    @Override
    public void before() throws Exception {
        gridConfigProvider = new ImageGridConfigProvider();
        isTablet = false;
    }


    @Test
    public void testItemHeight() {
        assertEquals(762, gridConfigProvider.getItemHeight(2, 110, 1080, isTablet));
        assertEquals(762, gridConfigProvider.getItemHeight(2, 150, 1080, isTablet));
        assertEquals(381, gridConfigProvider.getItemHeight(1, 120, 1080, isTablet));
        assertEquals(381, gridConfigProvider.getItemHeight(1, 121, 1080, isTablet));
        assertEquals(1524, gridConfigProvider.getItemHeight(2, 60, 1080, isTablet));
        assertEquals(1524, gridConfigProvider.getItemHeight(2, 80, 1080, isTablet));
        assertEquals(762, gridConfigProvider.getItemHeight(1, 20, 1080, isTablet));
        assertEquals(762, gridConfigProvider.getItemHeight(1, 90, 1080, isTablet));
    }

    @Test
    public void testFiveLandImages() {
        final int[] ratio = {110, 120, 130, 140, 150, 160};
        testFiveLandSpan(testFiveLandImages(ratio));
        final int[] ratio1 = {110, 120, 130, 140, 150, 60};
        testFiveLandSpan(testFiveLandImages(ratio1));
        final int[] ratio2 = {10, 120, 130, 140, 150, 160};
        testFiveLandSpan(testFiveLandImages(ratio2));
        final int[] ratio3 = {110, 120, 130, 40, 150, 160};
        testFiveLandSpan(testFiveLandImages(ratio3));
    }

    private GridConfig testFiveLandImages(final int[] ratio) {
        final GridConfig res = gridConfigProvider.getGridConfigByRatio(ratio);
        printResult(res);
        final int[] positions = res.getPositions();
        assertEquals(positions.length, ratio.length);
        assertEquals(ImageGridConfigProvider.TYPE_FIVE_LAND, res.getGridType());
        int nonNullItems = 0;
        for (int i = 0; i < positions.length; i++) {
            final int pos = positions[i];
            if (pos >= 0) {
                assertTrue(ratio[i] > 100);
                nonNullItems++;
            }
        }
        assertEquals(5, nonNullItems);
        return res;
    }

    protected void testSpan(final GridConfig config, final Map<Integer, Integer> expectedSpanByPosition) {
        final int type = config.getGridType();
        for (final int pos : config.getPositions()) {
            if (expectedSpanByPosition.containsKey(pos)) {
                final int expectedSpan = expectedSpanByPosition.get(pos);
                final int span = gridConfigProvider.getItemSpan(type, pos, isTablet);
                assertEquals(expectedSpan, span);
            } else {
                try {
                    gridConfigProvider.getItemSpan(type, pos, isTablet);
                    fail();
                } catch (IllegalArgumentException e) {
                }
            }
        }
    }

    private void testFiveLandSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            final int span;
            if (i == 0) {
                span = 2;
            } else if (i == 5) {
                span = -1;
            } else {
                span = 1;
            }
            expectedSpans.put(i, span);
        }
        testSpan(config, expectedSpans);
    }

    private void printResult(final GridConfig res) {
        System.out.println("Grid Type: " + res.getGridType());
        for (final Integer info : res.getPositions()) {
            System.out.print(info + ", ");
        }
        System.out.println();
    }

    @Test
    public void testThreeLandOnePortImages() {
        final int[] ratio = {110, 120, 130, 140, 50};
        testThreeLandOnePortSpan(testThreeLandOnePortImages(ratio));
        final int[] ratio1 = {10, 120, 130, 140, 150};
        testThreeLandOnePortSpan(testThreeLandOnePortImages(ratio1));
        final int[] ratio2 = {110, 120, 30, 140, 150};
        testThreeLandOnePortSpan(testThreeLandOnePortImages(ratio2));
        final int[] ratio3 = {110, 120, 130, 40};
        testThreeLandOnePortSpan(testThreeLandOnePortImages(ratio3));
        final int[] ratio4 = {10, 120, 130, 140};
        testThreeLandOnePortSpan(testThreeLandOnePortImages(ratio4));
        final int[] ratio5 = {110, 120, 30, 140};
        testThreeLandOnePortSpan(testThreeLandOnePortImages(ratio5));
        final int[] ratio6 = {110, 120, 130, 40, 50};
        testThreeLandOnePortSpan(testThreeLandOnePortImages(ratio6));
        final int[] ratio7 = {10, 120, 130, 140, 50};
        testThreeLandOnePortSpan(testThreeLandOnePortImages(ratio7));
        final int[] ratio8 = {110, 20, 130, 40, 150};
        testThreeLandOnePortSpan(testThreeLandOnePortImages(ratio8));
    }

    private void testThreeLandOnePortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            final int span;
            if (i == 0) {
                span = 2;
            } else if (i == 4) {
                span = -1;
            } else {
                span = 1;
            }
            expectedSpans.put(i, span);
        }
        testSpan(config, expectedSpans);
    }

    private GridConfig testThreeLandOnePortImages(final int[] ratio) {
        final GridConfig res = gridConfigProvider.getGridConfigByRatio(ratio);
        printResult(res);
        final int[] positions = res.getPositions();
        assertEquals(positions.length, ratio.length);
        assertEquals(ImageGridConfigProvider.TYPE_THREE_LAND_ONE_PORT, res.getGridType());
        int nonNullItems = 0;
        for (int i = 0; i < positions.length; i++) {
            final int pos = positions[i];
            if (pos >= 0) {
                if (pos == 1) {
                    assertTrue(ratio[i] < 100);
                } else {
                    assertTrue(ratio[i] > 100);
                }
                nonNullItems++;
            }
        }
        assertEquals(4, nonNullItems);
        return res;
    }

    @Test
    public void testTwoLandThreePortImages() {
        final int[] ratio = {110, 120, 40, 50, 60};
        testTwoLandThreePortSpan(testTwoLandThreePortImages(ratio));
        final int[] ratio2 = {40, 50, 60, 110, 120};
        testTwoLandThreePortSpan(testTwoLandThreePortImages(ratio2));
        final int[] ratio3 = {110, 40, 120, 50, 60};
        testTwoLandThreePortSpan(testTwoLandThreePortImages(ratio3));
    }

    private void testTwoLandThreePortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, 1);
        }
        testSpan(config, expectedSpans);
    }

    private GridConfig testTwoLandThreePortImages(final int[] ratio) {
        final GridConfig res = gridConfigProvider.getGridConfigByRatio(ratio);
        printResult(res);
        final int[] positions = res.getPositions();
        assertEquals(positions.length, ratio.length);
        assertEquals(ImageGridConfigProvider.TYPE_TWO_LAND_THREE_PORT, res.getGridType());

        int nonNullItems = 0;
        for (int i = 0; i < positions.length; i++) {
            final int pos = positions[i];
            if (pos >= 0) {
                if (pos <= 2) {
                    assertTrue(ratio[i] < 100);
                } else {
                    assertTrue(ratio[i] > 100);
                }
                nonNullItems++;
            }
        }
        assertEquals(5, nonNullItems);

        return res;
    }

    @Test
    public void testTwoLandTwoPortImages() {
        final int[] ratio = {110, 40, 130, 40};
        testTwoLandTwoPortSpan(testTwoLandTwoPortImages(ratio));
        final int[] ratio2 = {30, 40, 120, 110};
        testTwoLandTwoPortSpan(testTwoLandTwoPortImages(ratio2));
        final int[] ratio3 = {110, 120, 30, 40};
        testTwoLandTwoPortSpan(testTwoLandTwoPortImages(ratio3));
    }

    private void testTwoLandTwoPortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, i < 4 ? 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    private GridConfig testTwoLandTwoPortImages(final int[] ratio) {
        final GridConfig res = gridConfigProvider.getGridConfigByRatio(ratio);
        printResult(res);
        final int[] positions = res.getPositions();
        assertEquals(positions.length, ratio.length);
        assertEquals(ImageGridConfigProvider.TYPE_TWO_LAND_TWO_PORT, res.getGridType());
        int nonNullItems = 0;
        for (int i = 0; i < positions.length; i++) {
            final int pos = positions[i];
            if (pos >= 0) {
                if (pos == 0 || pos == 1) {
                    assertTrue(ratio[i] < 100);
                } else {
                    assertTrue(ratio[i] > 100);
                }
                nonNullItems++;
            }
        }
        assertEquals(4, nonNullItems);
        return res;
    }

    @Test
    public void testFourPortImages() {
        final int[] ratio = {110, 20, 30, 40, 50};
        testFourPortSpan(testFourPortImages(ratio));
        final int[] ratio2 = {10, 20, 30, 40, 50};
        testFourPortSpan(testFourPortImages(ratio2));
        final int[] ratio3 = {10, 20, 30, 40};
        testFourPortSpan(testFourPortImages(ratio3));
    }

    private void testFourPortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, i < 4 ? 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    public GridConfig testFourPortImages(final int[] ratio) {
        final GridConfig res = gridConfigProvider.getGridConfigByRatio(ratio);
        printResult(res);
        final int[] positions = res.getPositions();
        assertEquals(positions.length, ratio.length);
        assertEquals(ImageGridConfigProvider.TYPE_FOUR_PORT, res.getGridType());
        int nonNullItems = 0;
        for (int i = 0; i < positions.length; i++) {
            final int pos = positions[i];
            if (pos >= 0) {
                assertTrue(ratio[i] < 100);
                nonNullItems++;
            }
        }
        assertEquals(4, nonNullItems);
        return res;
    }

    private GridConfig commonCheck(final int[] ratio,
                                   final int expectedNonNullItems,
                                   final int expectedGridType) {
        final GridConfig res = gridConfigProvider.getGridConfigByRatio(ratio);
        printResult(res);
        final int[] positions = res.getPositions();
        assertEquals(positions.length, ratio.length);
        assertEquals(expectedGridType, res.getGridType());
        int nonNullItems = 0;
        for (final int pos : positions) {
            if (pos >= 0) {
                nonNullItems++;
            }
        }
        assertEquals(expectedNonNullItems, nonNullItems);
        return res;
    }

    @Test
    public void testFourLandImages() {
        final int[] ratio = {110, 120, 130, 140};
        testFourLandSpan(commonCheck(ratio, 4, ImageGridConfigProvider.TYPE_FOUR_LAND));
    }


    protected void testFourLandSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, i < 4 ? 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Test
    public void testThreeLandImages() {
        final int[] ratio = {110, 120, 130};
        testThreeLandSpan(commonCheck(ratio, 3, ImageGridConfigProvider.TYPE_THREE_LAND));
    }

    private void testThreeLandSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 3) ? (i == 0) ? 2 : 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Test
    public void testOneLandTwoPortImages() {
        final int[] ratio = {110, 20, 30, 40};
        testOneLandTwoPortSpan(testOneLandTwoPortImages(ratio));
        final int[] ratio2 = {110, 20, 30};
        testOneLandTwoPortSpan(testOneLandTwoPortImages(ratio2));
    }

    private void testOneLandTwoPortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 3) ? (i == 0) ? 2 : 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    private GridConfig testOneLandTwoPortImages(final int[] ratio) {
        final GridConfig res = gridConfigProvider.getGridConfigByRatio(ratio);
        printResult(res);
        final int[] positions = res.getPositions();
        assertEquals(positions.length, ratio.length);
        assertEquals(ImageGridConfigProvider.TYPE_ONE_LAND_TWO_PORT, res.getGridType());
        int nonNullItems = 0;
        for (int i = 0; i < positions.length; i++) {
            final int pos = positions[i];
            if (pos >= 0) {
                if (pos == 0) {
                    assertTrue(ratio[i] > 100);
                } else {
                    assertTrue(ratio[i] < 100);
                }
                nonNullItems++;
            }
        }
        assertEquals(3, nonNullItems);
        return res;
    }

    @Test
    public void testTwoLandOnePortImages() {
        final int[] ratio = {110, 120, 30};
        testTwoLandOnePortSpan(testTwoLandOnePortImages(ratio));
        final int[] ratio1 = {10, 120, 130};
        testTwoLandOnePortSpan(testTwoLandOnePortImages(ratio1));
        final int[] ratio2 = {110, 20, 130};
        testTwoLandOnePortSpan(testTwoLandOnePortImages(ratio2));
    }

    protected void testTwoLandOnePortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 3) ? 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    private GridConfig testTwoLandOnePortImages(final int[] ratio) {
        final GridConfig res = gridConfigProvider.getGridConfigByRatio(ratio);
        printResult(res);
        final int[] positions = res.getPositions();
        assertEquals(positions.length, ratio.length);
        assertEquals(ImageGridConfigProvider.TYPE_TWO_LAND_ONE_PORT, res.getGridType());
        int nonNullItems = 0;
        for (int i = 0; i < positions.length; i++) {
            final int pos = positions[i];
            if (pos >= 0) {
                if (pos == 0) {
                    assertTrue(ratio[i] < 100);
                } else {
                    assertTrue(ratio[i] > 100);
                }
                nonNullItems++;
            }
        }
        assertEquals(3, nonNullItems);
        return res;
    }

    @Test
    public void testThreePortImages() {
        final int[] ratio = {10, 20, 30};
        testThreePortSpan(commonCheck(ratio, 3, ImageGridConfigProvider.TYPE_THREE_PORT));
    }

    protected void testThreePortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 3) ? (i == 0) ? 2 : 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Test
    public void testTwoPortImages() {
        final int[] ratio = {10, 20};
        testTwoPortSpan(commonCheck(ratio, 2, ImageGridConfigProvider.TYPE_TWO_PORT));
    }

    private void testTwoPortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 2) ? 1 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Test
    public void testTwoLandImages() {
        final int[] ratio = {110, 120};
        testTwoLandSpan(commonCheck(ratio, 2, ImageGridConfigProvider.TYPE_TWO_LAND));
    }

    private void testTwoLandSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 2) ? 2 : -1);
        }
        testSpan(config, expectedSpans);
    }

    @Test
    public void testOnePortOneLandImages() {
        final int[] ratio = {10, 120};
        testOnePortOneLandSpan(testOnePortOneLandImages(ratio));
        final int[] ratio2 = {110, 20};
        testOnePortOneLandSpan(testOnePortOneLandImages(ratio2));
    }

    protected void testOnePortOneLandSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, (i < 2) ? 2 : -1);
        }
        testSpan(config, expectedSpans);
    }

    private GridConfig testOnePortOneLandImages(final int[] ratio) {
        final GridConfig res = gridConfigProvider.getGridConfigByRatio(ratio);
        final int[] positions = res.getPositions();
        printResult(res);
        assertEquals(positions.length, ratio.length);
        assertEquals(ImageGridConfigProvider.TYPE_ONE_LAND_ONE_PORT, res.getGridType());
        int nonNullItems = 0;
        for (int i = 0; i < positions.length; i++) {
            final int pos = positions[i];
            if (pos >= 0) {
                if (pos == 0) {
                    assertTrue(ratio[i] > 100);
                } else {
                    assertTrue(ratio[i] < 100);
                }
                nonNullItems++;
            }
        }
        assertEquals(2, nonNullItems);
        return res;
    }

    @Test
    public void testSingleImage() {
        final int[] ratio = {10};
        testOnePortSpan(commonCheck(ratio, 1, ImageGridConfigProvider.TYPE_ONE_PORT));

        final int[] ratio2 = {110};
        testOneLandSpan(commonCheck(ratio2, 1, ImageGridConfigProvider.TYPE_ONE_LAND));
    }

    protected void testOnePortSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, i == 0 ? 2 : -1);
        }
        testSpan(config, expectedSpans);
    }

    protected void testOneLandSpan(final GridConfig config) {
        final Map<Integer, Integer> expectedSpans = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            expectedSpans.put(i, i == 0 ? 2 : -1);
        }
        testSpan(config, expectedSpans);
    }
}
