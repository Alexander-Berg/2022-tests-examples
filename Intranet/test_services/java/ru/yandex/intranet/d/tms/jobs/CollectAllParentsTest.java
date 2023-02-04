package ru.yandex.intranet.d.tms.jobs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.intranet.d.util.Long2LongMultimap;

/**
 * Test traversing the tree to collect parents of all levels.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 04.11.2020
 */
public class CollectAllParentsTest {
    @Test
    void collectAllParentsTest() {
        Long2LongMultimap serviceIdsByParentId = new Long2LongMultimap() {{
            put(0, 1);
            put(0, 2);
            put(1, 3);
            put(1, 4);
            put(3, 5);
        }};

        Long2LongMultimap allParents = CollectServicesParents.collectAllParents(serviceIdsByParentId);

        Assertions.assertEquals(
                new Long2LongMultimap() {{
                    put(1, 0);
                    put(2, 0);
                    put(3, 1);
                    put(3, 0);
                    put(4, 1);
                    put(4, 0);
                    put(5, 3);
                    put(5, 1);
                    put(5, 0);
                }},
                allParents
        );
    }
}
