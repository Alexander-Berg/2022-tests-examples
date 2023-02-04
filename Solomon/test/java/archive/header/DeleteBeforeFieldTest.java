package ru.yandex.solomon.codec.archive.header;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class DeleteBeforeFieldTest {

    private void testMerge(long expected, long a, long b) {
        Assert.assertEquals(expected, DeleteBeforeField.merge(a, b));
    }

    @Test
    public void merge() {
        long ts0 = Instant.parse("2016-03-19T02:41:33Z").toEpochMilli();

        testMerge(DeleteBeforeField.KEEP, DeleteBeforeField.KEEP, DeleteBeforeField.KEEP);
        testMerge(DeleteBeforeField.DELETE_ALL, DeleteBeforeField.DELETE_ALL, DeleteBeforeField.DELETE_ALL);
        testMerge(ts0, ts0, ts0);

        testMerge(DeleteBeforeField.DELETE_ALL, DeleteBeforeField.DELETE_ALL, DeleteBeforeField.KEEP);
        testMerge(DeleteBeforeField.DELETE_ALL, DeleteBeforeField.KEEP, DeleteBeforeField.DELETE_ALL);
        testMerge(DeleteBeforeField.DELETE_ALL, ts0, DeleteBeforeField.DELETE_ALL);
        testMerge(DeleteBeforeField.DELETE_ALL, DeleteBeforeField.DELETE_ALL, ts0);

        testMerge(ts0, DeleteBeforeField.KEEP, ts0);
        testMerge(ts0, ts0, DeleteBeforeField.KEEP);

        testMerge(ts0, ts0, ts0 - 1000);
        testMerge(ts0, ts0 - 1000, ts0);
    }

}
