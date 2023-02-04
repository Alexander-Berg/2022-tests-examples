package ru.yandex.solomon.model.array.mh;

import java.lang.invoke.MethodHandle;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.bolts.type.array.ArrayType;

/**
 * @author Stepan Koltsov
 */
@ParametersAreNonnullByDefault
public class ArraySimpleFieldMhTest extends ArrayLikeFieldMhTest {

    @Override
    protected <A, E> ArrayLikeFieldMh<A, E> newArrayLike(ArrayType<A, E> arrayType) {
        return new ArraySimpleFieldMh<>(arrayType);
    }

    @Override
    protected <A, E> Object constantsArray(ArrayType<A, E> arrayType, E element, int length) {
        return arrayType.filled(length, element);
    }

    @Override
    protected <A, E> Object arrayOf(ArrayType<A, E> arrayType, E[] elements) {
        return arrayType.withElements(elements);
    }

    @Override
    protected <A, E> Object emptyArray(ArrayType<A, E> arrayType) {
        return arrayType.emptyArray();
    }

    @Override
    protected <A, E> void assertConstants(ArrayType<A, E> arrayType, Object array, int len, E c) {
        for (int i = 0; i < len; ++i) {
            Assert.assertEquals(c, arrayType.getElement((A) array, i));
        }
    }

    @Test
    public void copyOfIfNotEmpty() throws Throwable {
        MethodHandle mh = new ArraySimpleFieldMh<>(long[].class).copyOfIfNotEmpty();

        long[] emptyLongArray = new long[0];

        Assert.assertSame(emptyLongArray, (long[]) mh.invokeExact(emptyLongArray, 0));
        Assert.assertSame(emptyLongArray, (long[]) mh.invokeExact(emptyLongArray, 10));

        long[] a = new long[] { 0, 10, 20 };

        Assert.assertArrayEquals(new long[0], (long[]) mh.invokeExact(a, 0));
        Assert.assertArrayEquals(new long[] { 0, 10 }, (long[]) mh.invokeExact(a, 2));
        Assert.assertArrayEquals(new long[] { 0, 10, 20 }, (long[]) mh.invokeExact(a, 3));
        Assert.assertNotSame(a, (long[]) mh.invokeExact(a, a.length));
        Assert.assertArrayEquals(new long[] { 0, 10, 20, 0 }, (long[]) mh.invokeExact(a, 4));
    }
}
