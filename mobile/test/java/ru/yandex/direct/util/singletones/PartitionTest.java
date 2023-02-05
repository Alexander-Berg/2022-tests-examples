package ru.yandex.direct.util.singletones;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class PartitionTest {
    @Test
    public void allPartitionsAreEqual_ifHasEnoughElements() {
        testPartition(
                asList(1, 2, 3, 4, 5, 6),
                3,
                asList(1, 2, 3),
                asList(4, 5, 6)
        );
    }

    @Test
    public void lastPartitionIsShort_ifHasNotEnoughElements() {
        testPartition(
                asList(1, 2, 3, 4, 5),
                3,
                asList(1, 2, 3),
                asList(4, 5)
        );
    }

    @Test
    public void singlePartition_ifOnlyOneElementInList() {
        testPartition(
                singletonList(1),
                3,
                singletonList(1)
        );
    }

    @Test
    public void singlePartition_ifListSizeEqualToArgSize() {
        testPartition(
                asList(1, 2, 3),
                3,
                asList(1, 2, 3)
        );
    }

    @Test
    public void emptyPartition_ifEmptySource() {
        testPartition(
                asList(new Integer[0]),
                3,
                asList(new Integer[0])
        );
    }

    @Test
    public void partitionsAreSingleton_ifSizeEqualsToOne() {
        testPartition(
                asList(1, 2, 3),
                1,
                singletonList(1),
                singletonList(2),
                singletonList(3)
        );
    }

    @Test
    public void getPartitionsCount_worksCorrectly() {
        assertThat(CollectionUtils.getPartitionsCount(10, 10)).isEqualTo(1);
        assertThat(CollectionUtils.getPartitionsCount(20, 10)).isEqualTo(2);
        assertThat(CollectionUtils.getPartitionsCount(20, 11)).isEqualTo(2);
        assertThat(CollectionUtils.getPartitionsCount(20, 1)).isEqualTo(20);
        assertThat(CollectionUtils.getPartitionsCount(0, 10)).isEqualTo(0);
        assertThat(CollectionUtils.getPartitionsCount(0, 1)).isEqualTo(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsException_ifSizeEqualsToZero() {
        testPartition(
                asList(1, 2, 3),
                0
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsException_ifSizeLessThanZero() {
        testPartition(
                asList(1, 2, 3),
                -42
        );
    }

    @Test(expected = Exception.class)
    public void throwsException_ifSourceIsNull() {
        testPartition(
                null,
                3
        );
    }

    @SafeVarargs
    private final void testPartition(List<Integer> source, int size, List<Integer>... expectedResult) {
        List<List<Integer>> actualResult = CollectionUtils.partition(source, size);

        Assert.assertEquals(expectedResult.length, actualResult.size());

        for (int i = 0; i < actualResult.size(); ++i) {
            assertEquals(expectedResult[i], actualResult.get(i));
        }
    }

    private static void assertEquals(List<Integer> expected, List<Integer> actual) {
        Assert.assertArrayEquals(
                expected.toArray(new Integer[expected.size()]),
                actual.toArray(new Integer[actual.size()])
        );
    }
}
