package ru.yandex.solomon.labels.intern;

import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

import ru.yandex.monlib.metrics.labels.Label;


/**
 * @author Sergey Polovko
 */
public class InterningLabelAllocatorTest {

    @Test
    public void selfSize() {
        InterningLabelAllocator a = new InterningLabelAllocator();

        Label[] labels = new Label[10000];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = a.alloc("a" + i, "b" + i);
        }

        long estimated = a.memorySizeIncludingSelf();
        long precise = GraphLayout.parseInstance(a).totalSize();

        assertSize(precise, estimated, 0.2);
    }

    @Test
    public void evictWeekReference() throws InterruptedException {
        final int total = 10000;
        InterningLabelAllocator a = new InterningLabelAllocator();

        Cleaner cleaner = Cleaner.create();
        AtomicInteger cleanedCount = new AtomicInteger(0);

        // keep strong references to prevent labels eviction
        // before calculating precise size
        Label[] labels = new Label[total];

        for (int i = 0; i < total; i++) {
            Label label = a.alloc("a" + i, "b" + i);
            cleaner.register(label, cleanedCount::incrementAndGet);
            labels[i] = label;
        }

        long precise = GraphLayout.parseInstance(a).totalSize();
        long estimated = a.memorySizeIncludingSelf();
        assertSize(precise, estimated, 0.2);

        // drop strong references
        labels = null;

        // massage GC while all labels are not cleaned
        while (cleanedCount.get() != total) {
            System.gc();
            Thread.sleep(10);
        }

        // check one more time
        labels = new Label[total];
        for (int i = 0; i < total; i++) {
            labels[i] = a.alloc("aa" + i, "bb" + i);
        }

        estimated = a.memorySizeIncludingSelf();
        precise = GraphLayout.parseInstance(a).totalSize();
        assertSize(precise, estimated, 0.2);
    }

    private void assertSize(long precise, long estimated, double error) {
        double e = (double) Math.abs(estimated - precise) / precise;

        String message = String.format("estimated(%d) vs precise(%d), error %f", estimated, precise, error);
        Assert.assertTrue(message, e < error);
    }
}
