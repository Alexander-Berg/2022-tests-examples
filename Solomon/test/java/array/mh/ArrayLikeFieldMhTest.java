package ru.yandex.solomon.model.array.mh;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.type.array.ArrayType;
import ru.yandex.commune.mh.builder.MhBuilder;
import ru.yandex.commune.mh.builder.MhFieldRef;

/**
 * @author Stepan Koltsov
 */
@ParametersAreNonnullByDefault
public abstract class ArrayLikeFieldMhTest {

    protected abstract <A, E> ArrayLikeFieldMh<A, E> newArrayLike(ArrayType<A, E> arrayType);

    protected abstract <A, E> Object constantsArray(ArrayType<A, E> arrayType, E element, int length);

    protected abstract <A, E> Object arrayOf(ArrayType<A, E> arrayType, E... elements);

    protected abstract <A, E> Object emptyArray(ArrayType<A, E> arrayType);

    protected abstract <A, E> void assertConstants(ArrayType<A, E> arrayType, Object array, int len, E c);

    @Test
    public void swapInConstant() throws Throwable {
        ArrayLikeFieldMh<boolean[], Boolean> arrayMh = newArrayLike(Cf.BooleanArray);

        for (boolean b : new boolean[] { true, false }) {
            Object array = constantsArray(Cf.BooleanArray, b, 100);

            Assert.assertEquals(b, arrayMh.get().invoke(array, 10));

            arrayMh.swap().invoke(array, 10, 20);

            for (int i = 0; i < 100; ++i) {
                Assert.assertEquals(b, arrayMh.get().invoke(array, i));
            }
        }
    }

    @Test
    public void swapInRegular() throws Throwable {
        Random random = new Random(10);

        ArrayLikeFieldMh<boolean[], Boolean> arrayMh = newArrayLike(Cf.BooleanArray);

        for (int i = 0; i < 100; ++i) {
            Boolean[] elements = new Boolean[1 + random.nextInt(100)];
            for (int j = 0; j < elements.length; ++j) {
                elements[j] = random.nextBoolean();
            }

            Object array = arrayOf(Cf.BooleanArray, elements);

            int a = random.nextInt(elements.length);
            int b = random.nextInt(elements.length);

            Cf.ObjectArray.swapElements(elements, a, b);
            arrayMh.swap().invoke(array, a, b);

            for (int j = 0; j < elements.length; ++j) {
                Assert.assertEquals(elements[j], arrayMh.get().invoke(array, j));
            }
        }
    }

    @Test
    public void getIfNotEmptyOrDefault() throws Throwable {
        MethodHandle mh = newArrayLike(Cf.BooleanArray).getIfNotEmptyOrDefault();

        Assert.assertEquals(false, (boolean) mh.invoke(emptyArray(Cf.BooleanArray), 2));
        Assert.assertEquals(true, (boolean) mh.invoke(arrayOf(Cf.BooleanArray, false, true, false), 1));
        Assert.assertEquals(false, (boolean) mh.invoke(constantsArray(Cf.BooleanArray, false, 5), 2));
        Assert.assertEquals(true, (boolean) mh.invoke(constantsArray(Cf.BooleanArray, true, 5), 2));

        try {
            boolean ignore = (boolean) mh.invoke(arrayOf(Cf.BooleanArray, true, false), 10);
            Assert.fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void setAndReturn() throws Throwable {
        Random random = new Random(10);

        ArrayLikeFieldMh<long[], Long> arrayLike = newArrayLike(Cf.LongArray);

        MethodHandle get = arrayLike.get();

        MethodHandle set = MhBuilder.oneshot4(
            arrayLike.arrayLikeType(), int.class, int.class, long.class,
            arrayLike::setAndReturn);

        for (int i = 0; i < 100; ++i) {
            int len = 1 + random.nextInt(20);
            long init = random.nextInt(10);

            Object array = constantsArray(Cf.LongArray, init, len);
            long[] check = Cf.LongArray.filled(len, init);


            for (int j = 0; j < 100; ++j) {
                int pos = random.nextInt(len);
                int value = random.nextInt(10);

                {
                    Assert.assertEquals(check[pos], get.invoke(array, pos));
                }

                {
                    array = set.invoke(array, len, pos, value);
                    check[pos] = value;
                }
            }
        }
    }
        @Test
    public void setInField() throws Throwable {
        Random random = new Random(10);

        ArrayLikeFieldMh<long[], Long> arrayLike = newArrayLike(Cf.LongArray);

        class ArrayFields {
            Object longs;

            ArrayFields(Object longs) {
                this.longs = longs;
            }
        }

        Field field = ArrayFields.class.getDeclaredField("longs");

        MethodHandle get = arrayLike.getFromField(field);

        MethodHandle set = MhBuilder.oneshot4(
            ArrayFields.class, int.class, int.class, long.class,
            (arrays, len, dst, value) -> arrayLike.setInField(new MhFieldRef(arrays, field), len, dst, value));

        for (int i = 0; i < 100; ++i) {
            int len = 1 + random.nextInt(20);
            long init = random.nextInt(10);

            ArrayFields arrays = new ArrayFields(constantsArray(Cf.LongArray, init, len));
            long[] check = Cf.LongArray.filled(len, init);

            for (int j = 0; j < 100; ++j) {
                int pos = random.nextInt(len);
                int value = random.nextInt(10);

                {
                    Assert.assertEquals(check[pos], get.invoke(arrays, pos));
                }

                {
                    set.invoke(arrays, len, pos, value);
                    check[pos] = value;
                }
            }
        }
    }

    @Test
    public void copyOf() throws Throwable {
        ArrayLikeFieldMh<long[], Long> a = newArrayLike(Cf.LongArray);

        MethodHandle copyOf = MhBuilder.oneshot2(a.arrayLikeType(), int.class, a::copyOf);

        {
            Object aa = copyOf.invoke(emptyArray(Cf.LongArray), 33);
            assertConstants(Cf.LongArray, aa, 33, 0L);
        }

        {
            copyOf.invoke(arrayOf(Cf.LongArray, 10L, 11L, 12L), 33);
        }

        {
            Object aa = copyOf.invoke(constantsArray(Cf.LongArray, 10L, 22), 33);
            assertConstants(Cf.LongArray, aa, 22, 10L);
        }

        {
            // copy of constants to empty
            Object aa = copyOf.invoke(constantsArray(Cf.LongArray, 15L, 7), 0);
            Assert.assertSame(Cf.LongArray.emptyArray(), aa);
        }
    }



    @Test
    public void arrayIsNotEmpty() throws Throwable {
        MethodHandle mh = newArrayLike(Cf.LongArray).arrayIsNotEmpty();

        Assert.assertTrue((boolean) mh.invoke(constantsArray(Cf.LongArray, 3L, 2)));
        Assert.assertFalse((boolean) mh.invoke(emptyArray(Cf.LongArray)));
        Assert.assertFalse((boolean) mh.invoke(arrayOf(Cf.LongArray)));
        Assert.assertTrue((boolean) mh.invoke(arrayOf(Cf.LongArray, 10L)));
        Assert.assertTrue((boolean) mh.invoke(arrayOf(Cf.LongArray, 10L, 20L)));
    }

}
