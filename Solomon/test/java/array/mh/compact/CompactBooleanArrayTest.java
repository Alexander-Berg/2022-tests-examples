package ru.yandex.solomon.model.array.mh.compact;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;

/**
 * @author Stepan Koltsov
 */
public class CompactBooleanArrayTest {

    @Test
    public void random() {
        Random random = new Random(18);

        for (int i = 0; i < 100; ++i) {
            boolean[] expected = new boolean[1 + random.nextInt(20)];
            Object array = false;

            for (int j = 0; j < 100; ++j) {
                {
                    int pos = random.nextInt(expected.length);
                    boolean a = expected[pos];
                    boolean b = CompactBooleanArray.get(array, pos);
                    Assert.assertEquals(a, b);
                }

                {
                    int pos = random.nextInt(expected.length);

                    boolean b = random.nextBoolean();
                    expected[pos] = b;
                    array = CompactBooleanArray.set(array, expected.length, pos, b);
                }

                {
                    int pos1 = random.nextInt(expected.length);
                    int pos2 = random.nextInt(expected.length);

                    Cf.BooleanArray.swapElements(expected, pos1, pos2);
                    CompactBooleanArray.swap(array, pos1, pos2);
                }

                {
                    int pos1 = random.nextInt(expected.length);
                    int pos2 = random.nextInt(expected.length);

                    expected[pos1] = expected[pos2];
                    CompactBooleanArray.copy(array, pos1, pos2);
                }
            }
        }
    }

    @Test
    public void isConst() {
        Assert.assertFalse(CompactBooleanArray.isConst(Cf.BooleanArray.emptyArray(), true));
        Assert.assertFalse(CompactBooleanArray.isConst(Cf.BooleanArray.emptyArray(), false));
        Assert.assertFalse(CompactBooleanArray.isConst(new boolean[] { true }, true));
        Assert.assertFalse(CompactBooleanArray.isConst(new boolean[] { false }, false));
        Assert.assertTrue(CompactBooleanArray.isConst(true, true));
        Assert.assertFalse(CompactBooleanArray.isConst(true, false));
        Assert.assertFalse(CompactBooleanArray.isConst(false, true));
        Assert.assertTrue(CompactBooleanArray.isConst(false, false));
    }

}
