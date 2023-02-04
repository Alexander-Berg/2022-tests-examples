package ru.yandex.solomon.model.array.mh;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;

import ru.yandex.bolts.type.array.ArrayType;

/**
 * @author Stepan Koltsov
 */
@ParametersAreNonnullByDefault
public class ArrayCompactFieldMhTest extends ArrayLikeFieldMhTest {

    @Override
    protected <A, E> ArrayLikeFieldMh<A, E> newArrayLike(ArrayType<A, E> arrayType) {
        return new ArrayCompactFieldMh<>(arrayType);
    }

    @Override
    protected <A, E> Object constantsArray(ArrayType<A, E> arrayType, E element, int length) {
        if (length == 0) {
            return arrayType.emptyArray();
        }
        return element;
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
        Assert.assertSame(arrayType.elementWrapperClass(), c.getClass());
    }

}
