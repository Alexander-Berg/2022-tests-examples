package ru.yandex.webmaster3.core.util.concurrent;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.util.RetryUtils;
import ru.yandex.webmaster3.core.util.concurrent.graph.*;
import ru.yandex.webmaster3.core.util.concurrent.graph.GraphExecutionBuilder.Queue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author avhaliullin
 */
public class GraphExecutionTest {
    @Test
    public void allInputShouldBeProcessedExactlyOnce() throws Exception {
        GraphExecutionBuilder builder = GraphExecutionBuilder.newBuilder("test");
        Set<Integer> input = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            input.add(i);
        }
        List<Integer> acc = new ArrayList<>();
        Queue<Integer> node = builder.<Integer>process(() -> acc::addAll).getInput();
        GraphExecution<Integer> execution = builder.build(node);
        execution.start();
        for (int item : input) {
            execution.put(item);
        }
        execution.doneWritingAndAwaitTermination();
        Assert.assertEquals(input.size(), acc.size());
        Assert.assertEquals(input, new HashSet<>(acc));
    }

    @Test
    public void multiplexingShouldWork() throws Exception {
        GraphExecutionBuilder builder = GraphExecutionBuilder.newBuilder("test");
        Set<Integer> input = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            input.add(i);
        }
        List<Integer> acc1 = new ArrayList<>();
        List<Integer> acc2 = new ArrayList<>();
        Queue<Integer> node1 = builder.<Integer>process(() -> acc1::addAll).getInput();
        Queue<Integer> node2 = builder.<Integer>process(() -> acc2::addAll).getInput();
        Queue<Integer> intNode = builder.process(GraphExecutionBuilder.multiplex(node1, node2), out -> BlockingBatchConsumer.<Integer, Integer>mapping(out, RetryUtils.never(), FailPolicy.THROW, x -> x)).getInput();
        GraphExecution<Integer> execution = builder.build(intNode);
        execution.start();
        for (int item : input) {
            execution.put(item);
        }
        execution.doneWritingAndAwaitTermination();
        Assert.assertEquals(input.size(), acc1.size());
        Assert.assertEquals(input, new HashSet<>(acc1));
        Assert.assertEquals(input.size(), acc2.size());
        Assert.assertEquals(input, new HashSet<>(acc2));
    }

    @Test
    public void allTasksMustCompletedAfterTermination() throws Exception {
        GraphExecutionBuilder builder = GraphExecutionBuilder.newBuilder("test");
        Set<Integer> input = new HashSet<>();
        int expectedTotal = 0;
        for (int i = 0; i < 100; i++) {
            input.add(i);
            expectedTotal += i;
        }
        final AtomicInteger total = new AtomicInteger();
        // sum all
        Queue<Integer> node1 = builder.<Integer>process(() -> batch -> {
            Thread.sleep(16);
            batch.forEach(total::addAndGet);
        }).name("summer").batchLimit(15).getInput();

        // minus 1
        Queue<Integer> node2 = builder.<Integer, Integer>process(node1, (GraphOutQueue<Integer> q) -> batch -> {
            Thread.sleep(36);
            for (int val : batch) {
                q.put(val - 1);
            }

        }).name("minuser").batchLimit(15).sharded(10, val -> 0)
                .getInput();

        // plus 1
        Queue<Integer> node3 = builder.<Integer, Integer>process(node2, (GraphOutQueue<Integer> q) -> batch -> {
            for (int val : batch) {
                q.put(val + 1);
            }
        }).name("pluser").batchLimit(10).getInput();

        GraphExecution<Integer> graph = builder.build(node3);
        graph.start();
        for (Integer val : input) {
            graph.put(val);
        }
        graph.doneWritingAndAwaitTermination();

        // check sum
        Assert.assertEquals(expectedTotal, total.get());
    }
}
