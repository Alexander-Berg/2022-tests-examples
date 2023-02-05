package ru.yandex.disk.feed;

import androidx.collection.LongSparseArray;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.test.Assert2.*;

public class FeedViewItemsIdGeneratorTest {
    private static final int N = 1024;
    private static final int M = 1024;
    private static final int V = 20;

    @Test
    public void shouldBeStrongAgainstCollisions() throws Exception {
        final LongSparseArray<String> uniqueIds = new LongSparseArray<String>() {
            @Override
            public void put(final long key, final String value) {
                final Object prev = get(key);
                if (prev != null) {
                    fail("\n" + value + "\nis  double of\n" + prev);
                }
                super.put(key, value);
            }
        };
        for (long blockId = 1; blockId < N; blockId++) {
            for (int serverOrder = 0; serverOrder < M; serverOrder++) {
                final long blockItemId =
                        FeedViewItemsIdGenerator.generateBlockItemId(blockId, serverOrder);
                uniqueIds.put(blockItemId, "blockItemId = " + blockItemId + " for blockId = "
                        + blockId + " serverOrder = " + serverOrder);
            }
            for (int viewType = 0; viewType < V; viewType++) {
                final long viewItemId =
                        FeedViewItemsIdGenerator.generateExtraViewId(blockId, viewType);
                uniqueIds.put(viewItemId, "viewItemId = " + viewItemId + " viewType = "
                        + viewType + " blockId = " + blockId);
            }
        }
    }

    @Test
    public void generateExtraViewId() throws Exception {
        assertThat(Long.toBinaryString(FeedViewItemsIdGenerator.generateExtraViewId(1, 2)),
                equalTo("1"
                        + "00011"
                        + "00000000000000000000000000000000"));

    }

    @Test
    public void generateBlockItemId() throws Exception {
        assertThat(Long.toBinaryString(FeedViewItemsIdGenerator.generateBlockItemId(8, 32)),
                equalTo("1000"
                        + "00000"
                        + "00000000000000000000000000100000"));

    }

}