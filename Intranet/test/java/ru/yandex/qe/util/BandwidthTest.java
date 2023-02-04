package ru.yandex.qe.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.util.units.Bandwidth;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author entropia
 */
public class BandwidthTest {
    @Test
    public void there_are_125_kilobytes_per_second_in_a_megabit_per_second() {
        Assertions.assertEquals(Bandwidth.ofMbps(1L).toKBps(), 125L);
    }

    @Test
    public void there_are_20_gigabits_per_second_in_a_2500_megabytes_per_second() {
        Assertions.assertEquals(Bandwidth.ofMBps(2500).toGbps(), 20L);
    }

    @Test
    public void multiply() {
        Assertions.assertEquals(
                Bandwidth.ofMbps(100L).multipliedBy(10L),
                Bandwidth.ofGbps(1L)
        );
    }

    @Test
    public void multiply_by_negative_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Bandwidth.ofMbps(50).multipliedBy(-2);
        });
    }

    @Test
    public void multiply_by_zero_is_allowed() {
        Assertions.assertTrue(Bandwidth.ofMbps(512).multipliedBy(0).isZero());
    }

    @Test
    public void divide() {
        Assertions.assertEquals(Bandwidth.ofMbps(300).dividedBy(2), Bandwidth.ofMbps(150));
    }

    @Test
    public void divide_by_zero_fails() {
        assertThrows(ArithmeticException.class, () -> {
            Bandwidth.ofMbps(100).dividedBy(0);
        });
    }

    @Test
    public void divide_by_negative_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Bandwidth.ofMbps(100).dividedBy(-5L);
        });
    }

    @Test
    public void scale() {
        Assertions.assertEquals(Bandwidth.ofMbps(100).scaledBy(5.0), Bandwidth.ofMbps(500));
    }

    @Test
    public void scale_by_nan_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Bandwidth.ofMbps(100).scaledBy(Double.NaN);
        });
    }

    @Test
    public void scale_by_plus_infinity_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Bandwidth.ofMbps(100).scaledBy(Double.POSITIVE_INFINITY);
        });
    }

    @Test
    public void scale_by_negative_infinity_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Bandwidth.ofMbps(100).scaledBy(Double.NEGATIVE_INFINITY);
        });
    }

    @Test
    public void scale_by_negative_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Bandwidth.ofMbps(100).scaledBy(-5.0);
        });
    }

    @Test
    public void equals() {
        Assertions.assertTrue(Bandwidth.ofMBps(2500).equals(Bandwidth.ofGbps(20)));
        Assertions.assertTrue(Bandwidth.ofGbps(20).equals(Bandwidth.ofMBps(2500)));
    }

    @Test
    public void hash() {
        final Set<Bandwidth> bandwidths = new LinkedHashSet<>();
        for (int i = 0; i < 100; i++) {
            bandwidths.add(Bandwidth.ofMBps(48));
        }

        Assertions.assertEquals(bandwidths.size(), 1);
        Assertions.assertEquals(bandwidths.iterator().next(), Bandwidth.ofMBps(48));
    }

    @Test
    public void compare_equal() {
        Assertions.assertEquals(Bandwidth.ofMBps(48).compareTo(Bandwidth.ofMBps(48)), 0);
    }

    @Test
    public void compare_less() {
        Assertions.assertEquals(Bandwidth.ofMBps(47).compareTo(Bandwidth.ofMBps(48)), -1);
    }

    @Test
    public void compare_more() {
        Assertions.assertEquals(Bandwidth.ofMBps(49).compareTo(Bandwidth.ofMBps(48)), 1);
    }

    @Test
    public void sort() {
        final SortedSet<Bandwidth> bandwidths = new TreeSet<>();
        bandwidths.add(Bandwidth.ofMBps(49));
        bandwidths.add(Bandwidth.ofMBps(48));
        bandwidths.add(Bandwidth.ofMBps(47));
        bandwidths.add(Bandwidth.ofMBps(48)); // duplicate

        MatcherAssert.assertThat(bandwidths, hasSize(3));
        MatcherAssert.assertThat(bandwidths, contains(
                Bandwidth.ofMBps(47),
                Bandwidth.ofMBps(48),
                Bandwidth.ofMBps(49)
        ));
    }
}
