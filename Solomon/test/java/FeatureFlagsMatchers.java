package ru.yandex.solomon.flags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class FeatureFlagsMatchers {

    public static void assertEmpty(FeatureFlags flags) {
        assertTrue(flags.toString(), flags.isEmpty());
    }

    public static void assertNotEmpty(FeatureFlags flags) {
        assertFalse(flags.toString(), flags.isEmpty());
    }

    public static void assertIsDefined(FeatureFlags flags, FeatureFlag flag) {
        assertTrue(flags.toString(), flags.isDefined(flag));
    }

    public static void assertHasFlag(FeatureFlags flags, FeatureFlag flag) {
        assertTrue(flags.toString(), flags.hasFlag(flag));
    }

    public static void assertHasNotFlag(FeatureFlags flags, FeatureFlag flag) {
        assertFalse(flags.toString(), flags.hasFlag(flag));
    }
}
