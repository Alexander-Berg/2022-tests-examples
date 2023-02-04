package ru.yandex.qe.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.util.units.Space;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author entropia
 */
public class SpaceTest {
    @Test
    public void kilobytes_to_bytes() {
        Assertions.assertEquals(Space.ofKilobytes(256L).toBytes(), 256L << 10);
    }

    @Test
    public void megabytes_to_bytes() {
        Assertions.assertEquals(Space.ofMegabytes(500L).toBytes(), 500L << 20);
    }

    @Test
    public void gigabytes_to_bytes() {
        Assertions.assertEquals(Space.ofGigabytes(5L).toBytes(), 5L << 30);
    }

    @Test
    public void two_terabytes_as_megabytes_to_bytes() {
        Assertions.assertEquals(Space.ofMegabytes(Space.ofTerabytes(2L).toMegabytes()).toBytes(), 2L << 40);
    }

    @Test
    public void two_terabytes_to_megabytes() {
        Assertions.assertEquals(Space.ofTerabytes(2L).toMegabytes(), 2097152L);
    }

    @Test
    public void almost_three_gigabytes() {
        Assertions.assertEquals(
                Space.ofGigabytes(2L).toMegabytes() + Space.ofMegabytes(768L).toMegabytes(),
                2816L
        );
    }

    @Test
    public void multiply() {
        Assertions.assertEquals(
                Space.ofGigabytes(2L).multipliedBy(5L),
                Space.ofGigabytes(10L)
        );
    }

    @Test
    public void multiply_by_negative_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Space.ofMegabytes(512).multipliedBy(-2);
        });
    }

    @Test
    public void scale() {
        Assertions.assertEquals(Space.ofMegabytes(512).scaledBy(2.0), Space.ofGigabytes(1));
    }

    @Test
    public void scale_by_nan_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Space.ofMegabytes(512).scaledBy(Double.NaN);
        });
    }

    @Test
    public void scale_by_plus_infinity_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Space.ofMegabytes(512).scaledBy(Double.POSITIVE_INFINITY);
        });
    }

    @Test
    public void scale_by_negative_infinity_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Space.ofMegabytes(512).scaledBy(Double.NEGATIVE_INFINITY);
        });
    }

    @Test
    public void scale_by_negative_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Space.ofMegabytes(512).scaledBy(-5.0);
        });
    }

    @Test
    public void multiply_by_zero_is_allowed() {
        Assertions.assertTrue(Space.ofMegabytes(512).multipliedBy(0).isZero());
    }

    @Test
    public void divide() {
        Assertions.assertEquals(Space.ofMegabytes(512).dividedBy(2), Space.ofMegabytes(256));
    }

    @Test
    public void divide_by_zero_fails() {
        assertThrows(ArithmeticException.class, () -> {
            Space.ofMegabytes(512).dividedBy(0);
        });
    }

    @Test
    public void divide_by_negative_fails() {
        assertThrows(IllegalArgumentException.class, () -> {
            Space.ofMegabytes(512).dividedBy(-5L);
        });
    }

    @Test
    public void plus() {
        Assertions.assertEquals(Space.ofMegabytes(512).plusKilobytes(1024), Space.ofMegabytes(513));
    }

    @Test
    public void plus_negative() {
        Assertions.assertEquals(Space.ofMegabytes(512).plusKilobytes(-1024), Space.ofMegabytes(511));
    }

    @Test
    public void plus_negative_fails_if_we_subtract_too_much() {
        assertThrows(IllegalArgumentException.class, () -> {
            Space.ofMegabytes(512).plusMegabytes(-512).plusBytes(-1);
        });
    }

    @Test
    public void minus_fails_if_we_subtract_too_much() {
        assertThrows(IllegalArgumentException.class, () -> {
            final Space _512M = Space.ofMegabytes(512);
            _512M.minus(_512M).minus(Space.ofBytes(1));
        });
    }

    @Test
    public void safeMinus_returns_zero_if_we_subtract_too_much() {
        final Space _512M = Space.ofMegabytes(512);
        Assertions.assertEquals(_512M.minus(_512M).safeMinus(Space.ofBytes(1)), Space.ZERO);
    }

    @Test
    public void subtract_negative() {
        Assertions.assertEquals(Space.ofMegabytes(512).minusMegabytes(-1), Space.ofMegabytes(513));
    }

    @Test
    public void diff() {
        Assertions.assertEquals(Space.ofMegabytes(512).difference(Space.ofMegabytes(256)), Space.ofMegabytes(512).minus(Space.ofMegabytes(256)));
        Assertions.assertEquals(Space.ofMegabytes(512).difference(Space.ofMegabytes(256)), Space.ofMegabytes(256));
    }

    @Test
    public void diff_zero() {
        Assertions.assertTrue(Space.ofMegabytes(512).difference(Space.ofMegabytes(512)).isZero());
    }

    @Test
    public void diff_small_minus_large_is_positive() {
        Assertions.assertEquals(Space.ofMegabytes(256).difference(Space.ofMegabytes(512)), Space.ofMegabytes(256));
    }

    @Test
    public void equals() {
        Assertions.assertTrue(Space.ofMegabytes(512).equals(Space.ofMegabytes(512)));
        Assertions.assertTrue(Space.ofKilobytes(512 << 10L).equals(Space.ofMegabytes(512)));
        Assertions.assertTrue(Space.ofMegabytes(512).equals(Space.ofKilobytes(512 << 10L)));
    }

    @Test
    public void hash() {
        final Set<Space> spaces = new LinkedHashSet<>();
        for (int i = 0; i < 100; i++) {
            spaces.add(Space.ofMegabytes(512));
        }
        MatcherAssert.assertThat(spaces, hasSize(1));
        MatcherAssert.assertThat(spaces, contains(Space.ofMegabytes(512)));
    }

    @Test
    public void compare_equal() {
        Assertions.assertEquals(Space.ofMegabytes(512).compareTo(Space.ofMegabytes(512)), 0);
    }

    @Test
    public void compare_less() {
        Assertions.assertTrue(Space.ofMegabytes(512).compareTo(Space.ofGigabytes(1)) < 0, "512M must be < 1G");
    }

    @Test
    public void compare_more() {
        Assertions.assertTrue(Space.ofMegabytes(512).compareTo(Space.ofMegabytes(256)) > 0, "512M must be > 256M");
    }

    @Test
    public void to_string_is_human_readable_zero() {
        Assertions.assertEquals(Space.ZERO.toString(), "0 bytes");
    }

    @Test
    public void to_string_is_human_readable_one_byte() {
        Assertions.assertEquals(Space.ofBytes(1).toString(), "1 byte");
    }

    @Test
    public void to_string_is_human_readable_less_than_one_kilobyte() {
        Assertions.assertEquals(Space.ofKilobytes(1).minusBytes(1).toString(), "1023 bytes");
    }

    @Test
    public void to_string_is_human_readable_one_kilobyte() {
        Assertions.assertEquals(Space.ofKilobytes(1).toString(), "1K (1024 bytes)");
    }

    @Test
    public void to_string_is_human_readable_one_kilobyte_plus_some_bytes() {
        Assertions.assertEquals(Space.ofKilobytes(1).plusBytes(2).toString(), "1K (1026 bytes)");
    }

    @Test
    public void sort() {
        final SortedSet<Space> sizes = new TreeSet<>();
        sizes.add(Space.ofMegabytes(512));
        sizes.add(Space.ofMegabytes(256));
        sizes.add(Space.ofMegabytes(512)); // duplicate
        sizes.add(Space.ofTerabytes(7));
        sizes.add(Space.ofMegabytes(322));
        sizes.add(Space.ofMegabytes(384));
        sizes.add(Space.ofMegabytes(148));
        sizes.add(Space.ofMegabytes(56));
        sizes.add(Space.ofGigabytes(5));

        MatcherAssert.assertThat(sizes, hasSize(8));
        MatcherAssert.assertThat(sizes, contains(
                Space.ofMegabytes(56),
                Space.ofMegabytes(148),
                Space.ofMegabytes(256),
                Space.ofMegabytes(322),
                Space.ofMegabytes(384),
                Space.ofMegabytes(512),
                Space.ofGigabytes(5),
                Space.ofTerabytes(7)
        ));
    }
}
