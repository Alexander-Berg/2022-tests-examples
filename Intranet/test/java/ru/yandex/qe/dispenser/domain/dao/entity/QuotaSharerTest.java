package ru.yandex.qe.dispenser.domain.dao.entity;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class QuotaSharerTest {
    @Test
    public void summaryQuotaMustBeEqualToSize() {
        final long size = 10;
        final long[] quotas = QuotaSharer.INSTANCE.share(10, new int[]{1, 1, 1});
        Assertions.assertEquals(size, Arrays.stream(quotas).sum());
    }

    @Test
    public void quotaSharingRegressionTest() {
        final long[] quotas = QuotaSharer.INSTANCE.share(6, new int[]{1, 1, 1, 1});
        Assertions.assertArrayEquals(new long[]{2, 2, 1, 1}, quotas);
    }

    @Test
    public void oneUnitSharingTest() {
        final long[] quotas = QuotaSharer.INSTANCE.share(1, new int[]{1, 1, 1});
        Assertions.assertArrayEquals(new long[]{1, 0, 0}, quotas);
    }

    @Test
    public void residualMustBeSharedBetweenFirstProjects() {
        final long[] quotas = QuotaSharer.INSTANCE.share(20L, new int[]{1, 1, 2, 3});
        System.out.println(Arrays.toString(quotas));
        Assertions.assertArrayEquals(new long[]{3, 3, 6, 8}, quotas);
    }
}