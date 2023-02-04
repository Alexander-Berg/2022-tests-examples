package ru.yandex.realty.misc.enums.flag;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Stepan Koltsov
 */
public class FlagUtilsTest extends TestCase {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(FlagUtilsTest.class);

    public void testIsValidBitMask() {
        assertTrue(FlagUtils.isValidBitMask(0x0001));
        assertTrue(FlagUtils.isValidBitMask(0x0002));
        assertTrue(FlagUtils.isValidBitMask(0x0004));
        assertTrue(FlagUtils.isValidBitMask(0x0080));
        assertTrue(FlagUtils.isValidBitMask(0x4000));
        assertTrue(FlagUtils.isValidBitMask(0x40000000));
        assertTrue(FlagUtils.isValidBitMask(0x0000200000000000L));
        assertTrue(FlagUtils.isValidBitMask(0x8000000000000000L));

        assertFalse(FlagUtils.isValidBitMask(0));
        assertFalse(FlagUtils.isValidBitMask(3));
        assertFalse(FlagUtils.isValidBitMask(0x000010001));
    }

    public void testGetPositionByMask() {
        assertEquals(0, FlagUtils.getPositionByMask(1));
        assertEquals(1, FlagUtils.getPositionByMask(2));
        assertEquals(2, FlagUtils.getPositionByMask(4));
        assertEquals(3, FlagUtils.getPositionByMask(8));
        assertEquals(4, FlagUtils.getPositionByMask(16));
        assertEquals(5, FlagUtils.getPositionByMask(32));
        assertEquals(6, FlagUtils.getPositionByMask(64));
        assertEquals(63, FlagUtils.getPositionByMask(0x8000000000000000L));
    }

    public void testSimpleFlag() {
        Set<F> f = new HashSet();
        f.add(F.V1);
        f.add(F.V2);

        long mask = FlagUtils.getMask(f);
        assertEquals(3, mask);

        Set<F> fs = FlagUtils.getFlagSet(F.R, mask);
        assertEquals(f, fs);
    }

    enum F implements Flag {
        V1(0x01),
        V2(0x02);

        private final int position;

        private F(int mask) {
            this.mask = mask;
            this.position = FlagUtils.getPositionByMask(mask);
        }

        private final int mask;

        @Override
        public int position() {
            return position;
        }

        @Override
        public long mask() {
            return mask;
        }

        public static FlagResolver<F> R = FlagResolver.r(F.class);
    }
} //~
