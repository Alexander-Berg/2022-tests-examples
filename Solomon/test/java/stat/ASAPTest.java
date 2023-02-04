package ru.yandex.solomon.math.stat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.model.timeseries.GraphData;
import ru.yandex.solomon.model.timeseries.GraphDataArrayList;
import ru.yandex.solomon.util.collection.array.DoubleArrayView;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class ASAPTest {

    @Test
    public void noVariance() {
        long ts0 = System.currentTimeMillis();

        GraphDataArrayList list = new GraphDataArrayList(100);
        for (int index = 0; index < 100; index++) {
            list.add(ts0 + (index * 15_000), 42.0);
        }

        GraphData source = list.buildGraphData();
        GraphData result = ASAP.smooth(source);
        assertEquals(source, result);
    }

    @Test
    public void oneChange() {
        long ts0 = System.currentTimeMillis();

        GraphDataArrayList list = new GraphDataArrayList(100);
        for (int index = 0; index < 100; index++) {
            list.add(ts0 + (index * 15_000), 42.0);
        }
        list.add(ts0 + 100 * 15_000, 50);

        GraphData source = list.buildGraphData();
        GraphData result = ASAP.smooth(source);
        GraphData expected = source.slice(5, source.length() - 1);
        assertEquals(expected, result.slice(0, result.length() - 1));
    }

    @Test
    public void empty() {
        GraphData source = GraphData.empty;
        GraphData result = ASAP.smooth(source);
        assertEquals(source, result);
    }

    @Test
    public void randomVariance() {
        long ts0 = System.currentTimeMillis();

        GraphDataArrayList list = new GraphDataArrayList(100);
        for (int index = 0; index < 100; index++) {
            double value = ThreadLocalRandom.current().nextDouble(40.0, 50.0);
            list.add(ts0 + (index * 15_000), value);
        }

        GraphData source = list.buildGraphData();
        GraphData result = ASAP.smooth(source);
        DoubleArrayView values = result.getValues();
        for (int index = 50; index < values.length(); index++) {
            Assert.assertEquals(45, values.at(index), 5.0);
        }
    }

    @Test
    public void periodicData() {
        long ts0 = System.currentTimeMillis();
        final int size = 1000;

        Random random = new Random();
        GraphDataArrayList list = new GraphDataArrayList(size);
        boolean up = true;
        int prev = 0;
        for (int index = 0; index < size; index++) {
            long ts = ts0 + (index * 15_000);

            if (up) {
                prev+=10;
                if (prev >= 50) {
                    up = false;
                }
            } else {
                prev-=10;
                if (prev <= 0) {
                    up = true;
                }
            }

            int noise = random.nextInt(2);
            list.add(ts, prev + noise);
        }

        GraphData source = list.buildGraphData();
        List<GraphData> parts = Arrays.asList(
                source.slice(0, size / 2),
                source.slice(size / 2, size));

        for (GraphData part : parts) {
            GraphData result = ASAP.smooth(part);
            DoubleArrayView values = result.getValues();
            for (int index = values.length(); index < values.length(); index++) {
                Assert.assertEquals(25, values.at(index), 1.0);
            }
        }
    }

    @Test
    public void compatibility() throws IOException {
        long ts0 = System.currentTimeMillis();
        var data = Resources.toString(Resources.getResource(ASAPTest.class, "taxi.txt"), StandardCharsets.UTF_8);
        GraphDataArrayList list = new GraphDataArrayList();
        var it = data.lines().iterator();
        while (it.hasNext()) {
            ts0 += 10_000;
            list.add(ts0, Integer.parseInt(it.next()));
        }

        GraphData source = list.buildGraphData();
        var result = ASAP.smooth(source);
        double[] values = result.getValues().copyOrArray();
        assertEquals(16300, values[500], 100);
        assertEquals(16200, values[1000], 100);
        assertEquals(16000, values[1500], 100);
        assertEquals(15970, values[1980], 100);
        assertEquals(15980, values[2000], 100);
        assertEquals(16100, values[2900], 100);
    }
}
