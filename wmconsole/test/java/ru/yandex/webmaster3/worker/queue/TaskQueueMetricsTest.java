package ru.yandex.webmaster3.worker.queue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class TaskQueueMetricsTest {

    @Test
    public void testComputeInstantRps0() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        Assert.assertEquals(0, TaskQueueMetrics.computeInstantRps(startTimes, msToNs(1001)), 0.00001);
    }

    @Test
    public void testComputeInstantRps1() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        startTimes.addLast(msToNs(500));
        Assert.assertEquals(1, TaskQueueMetrics.computeInstantRps(startTimes, msToNs(1001)), 0.00001);

    }

    @Test
    public void testComputeInstantRps2() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        startTimes.addLast(msToNs(250));
        startTimes.addLast(msToNs(500));
        Assert.assertEquals(2, TaskQueueMetrics.computeInstantRps(startTimes, msToNs(1001)), 0.00001);
    }

    @Test
    public void testComputeInstantRps3() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        startTimes.addLast(msToNs(250));
        startTimes.addLast(msToNs(500));
        startTimes.addLast(msToNs(750));
        Assert.assertEquals(3, TaskQueueMetrics.computeInstantRps(startTimes, msToNs(1001)), 0.00001);
    }

    @Test
    public void testComputeInstantRps2_1() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        startTimes.addLast(msToNs(250));
        startTimes.addLast(msToNs(500));
        startTimes.addLast(msToNs(1250));
        Assert.assertEquals(2, TaskQueueMetrics.computeInstantRps(startTimes, msToNs(1499)), 0.05);
    }

    @Test
    public void testComputeInstantRps_0_999() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        startTimes.addLast(msToNs(250));
        startTimes.addLast(msToNs(750));
        Assert.assertEquals(0.999, TaskQueueMetrics.computeInstantRps(startTimes, msToNs(1751)), 0.05);
        Assert.assertEquals(Lists.<Long>newArrayList(msToNs(750)), new ArrayList<>(startTimes));
    }

    @Test
    public void testComputeInstantRps_0_2() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        startTimes.addLast(msToNs(250));
        startTimes.addLast(msToNs(750));
        Assert.assertEquals(0.2, TaskQueueMetrics.computeInstantRps(startTimes, msToNs(5760)), 0.05);
        Assert.assertEquals(Lists.<Long>newArrayList(msToNs(750)), new ArrayList<>(startTimes));
    }

    @Test
    public void testComputeRpsOver5Min1() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        startTimes.add(msToNs(250));
        Assert.assertEquals(1.0, TaskQueueMetrics.computeRpsOver5Min(startTimes, msToNs(1250)), 0.05);
    }

    @Test
    public void testComputeRpsOver5Min2() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        startTimes.add(msToNs(250));
        startTimes.add(msToNs(950));
        Assert.assertEquals(2.0, TaskQueueMetrics.computeRpsOver5Min(startTimes, msToNs(1250)), 0.05);
    }

    @Test
    public void testComputeRpsOver5Min1_1() throws Exception {
        ArrayDeque<Long> startTimes = new ArrayDeque<>();
        startTimes.add(msToNs(250));
        startTimes.add(msToNs(500));
        double expected = 1000.0 / (5 * 60 * 1000 + 250);
        float actual = TaskQueueMetrics.computeRpsOver5Min(startTimes, msToNs(5 * 60 * 1000 + 250));
        Assert.assertEquals(expected, actual, 0.00005);
    }

    private static long msToNs(long second) {
        return TimeUnit.MILLISECONDS.toNanos(second);
    }
}
