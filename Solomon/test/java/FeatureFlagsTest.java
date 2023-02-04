package ru.yandex.solomon.flags;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.flags.FeatureFlag.TEST;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertHasFlag;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertHasNotFlag;

/**
 * @author Vladimir Gordiychuk
 */
public class FeatureFlagsTest {

    @Test
    public void empty() {
        FeatureFlags flags = new FeatureFlags();
        assertHasNotFlag(flags, TEST);
    }

    @Test
    public void negativeFlag() {
        FeatureFlags flags = new FeatureFlags();
        flags.add(TEST, false);
        assertHasNotFlag(flags, TEST);
    }

    @Test
    public void positiveFlag() {
        FeatureFlags flags = new FeatureFlags();
        flags.add(TEST, true);
        assertHasFlag(flags, TEST);
    }

    @Test
    public void combineEmpty() {
        FeatureFlags left = new FeatureFlags();
        FeatureFlags right = new FeatureFlags();
        left.combine(right);
        assertHasNotFlag(left, TEST);
    }

    @Test
    public void combineToEmptyNegative() {
        FeatureFlags left = new FeatureFlags();
        FeatureFlags right = new FeatureFlags();
        right.add(TEST, false);
        left.combine(right);
        assertHasNotFlag(left, TEST);
    }

    @Test
    public void combineToEmptyPositive() {
        FeatureFlags left = new FeatureFlags();
        FeatureFlags right = new FeatureFlags();
        right.add(TEST, true);
        left.combine(right);
        assertHasFlag(left, TEST);
    }

    @Test
    public void combineFromEmptyNegative() {
        FeatureFlags left = new FeatureFlags();
        left.add(TEST, false);
        FeatureFlags right = new FeatureFlags();
        left.combine(right);
        assertHasNotFlag(left, TEST);
    }

    @Test
    public void combineFromEmptyPositive() {
        FeatureFlags left = new FeatureFlags();
        left.add(TEST, true);
        FeatureFlags right = new FeatureFlags();
        left.combine(right);
        assertHasFlag(left, TEST);
    }

    @Test
    public void combineAvoidReplaceNegative() {
        FeatureFlags left = new FeatureFlags();
        left.add(TEST, true);
        FeatureFlags right = new FeatureFlags();
        right.add(TEST, false);

        left.combine(right);
        assertHasFlag(left, TEST);
    }

    @Test
    public void combineAvoidReplacePositive() {
        FeatureFlags left = new FeatureFlags();
        left.add(TEST, false);
        FeatureFlags right = new FeatureFlags();
        right.add(TEST, true);

        left.combine(right);
        assertHasNotFlag(left, TEST);
    }

    @Test
    public void clear() {
        FeatureFlags flags = new FeatureFlags();
        flags.add(TEST, true);
        assertHasFlag(flags, TEST);
        flags.clear();
        assertHasNotFlag(flags, TEST);
    }

    @Test
    public void hasAnyFlag() {
        FeatureFlags flags = new FeatureFlags();
        assertFalse(flags.toString(), flags.hasAnyFlag());
        flags.add(TEST, false);
        assertFalse(flags.toString(), flags.hasAnyFlag());
        flags.add(TEST, true);
        assertTrue(flags.toString(), flags.hasAnyFlag());
    }

    @Test
    public void serializeDeserialize() {
        {
            var flags = serializeDeserialize(FeatureFlags.EMPTY);
            assertHasNotFlag(flags, TEST);
            assertTrue(flags.isEmpty());
        }
        {
            var flags = serializeDeserialize(new FeatureFlags());
            assertHasNotFlag(flags, TEST);
            assertTrue(flags.isEmpty());
        }
        {
            var source = new FeatureFlags();
            source.add(TEST, true);
            var flags = serializeDeserialize(source);
            assertHasFlag(flags, TEST);
        }
        {
            var source = new FeatureFlags();
            source.add(TEST, false);
            var flags = serializeDeserialize(source);
            assertHasNotFlag(flags, TEST);
            assertFalse(flags.isEmpty());
        }
    }

    private FeatureFlags serializeDeserialize(FeatureFlags flags) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var result = mapper.writeValueAsString(flags);
            System.out.println(flags + " serialized as " + result);
            var r = mapper.readValue(result, FeatureFlags.class);
            assertEquals(flags.toString(), r.toString());
            return r;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
