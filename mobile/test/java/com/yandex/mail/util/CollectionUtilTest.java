package com.yandex.mail.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import androidx.collection.LongSparseArray;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public final class CollectionUtilTest {

    @Test
    public void modifiableSetOf1Element() {
        Set<String> set = CollectionUtil.modifiableSetOf("yo");
        assertThat(set).containsExactly("yo");
    }

    @Test
    public void modifiableSetOf3Elements() {
        Set<String> set = CollectionUtil.modifiableSetOf("yo", "wat", "the");
        assertThat(set).contains("yo", "wat", "the").hasSize(3);
    }

    @Test
    public void modifiableSetOfElementsShouldBeModifiable() {
        Set<String> set = CollectionUtil.modifiableSetOf("yo");

        set.add("what");
        assertThat(set).contains("yo", "what").hasSize(2);

        set.remove("yo");
        assertThat(set).containsExactly("what");
    }

    @Test
    public void unmodifiableSetOf1Element() {
        Set<String> set = CollectionUtil.unmodifiableSetOf("yo");
        assertThat(set).containsExactly("yo");
    }

    @Test
    public void unmodifiableSetOf3Elements() {
        Set<String> set = CollectionUtil.unmodifiableSetOf("yo", "wat", "the");
        assertThat(set).contains("yo", "wat", "the").hasSize(3);
    }

    @Test
    public void unmodifiableSetOfElementsShouldBeUnmodifiable() {
        Set<String> set = CollectionUtil.unmodifiableSetOf("hey", "you");

        try {
            set.add("modify me");
            failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
        } catch (UnsupportedOperationException expected) {
            // it's okay
        }

        try {
            set.remove("hey");
            failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
        } catch (UnsupportedOperationException expected) {
            // it's okay
        }
    }

    @Test
    public void modifiableSetFrom() {
        List<Integer> ints = asList(1, 2, 3);
        Set<String> strings = CollectionUtil.modifiableSetFrom(ints, Object::toString);
        assertThat(strings).containsOnly("1", "2", "3");
    }

    @Test
    public void unmodifiableSetFrom() {
        List<Integer> ints = asList(1, 2, 3);
        Set<String> strings = CollectionUtil.unmodifiableSetFrom(ints, Object::toString);
        assertThat(strings).containsOnly("1", "2", "3");
    }

    @Test
    public void unmodifiableSetFromShouldBeUnmodifiable() {
        List<Integer> ints = asList(1, 2, 3);
        Set<String> set = CollectionUtil.unmodifiableSetFrom(ints, Object::toString);

        try {
            set.add("modify me");
            failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
        } catch (UnsupportedOperationException expected) {
            // it's okay
        }

        try {
            set.remove("hey");
            failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
        } catch (UnsupportedOperationException expected) {
            // it's okay
        }
    }

    @Test
    public void concat_twoLists() {
        List<Integer> list1 = Arrays.asList(3, 2, 1);
        List<Integer> list2 = Arrays.asList(4, 5, 6);
        assertThat(CollectionUtil.concat(list1, list2)).containsExactly(3, 2, 1, 4, 5, 6);
    }

    @Test(expected = NullPointerException.class)
    public void concat_shouldThrowIfFirstListNull() {
        CollectionUtil.concat(null, new ArrayList<>());
    }

    @Test(expected = NullPointerException.class)
    public void concat_shouldThrowIfSecondListNull() {
        CollectionUtil.concat(new ArrayList<>(), null);
    }

    @Test(expected = NullPointerException.class)
    public void concat_shouldThrowIfBothListsNull() {
        CollectionUtil.concat(null, null);
    }

    @Test
    public void valuesFromSparseArray_shouldReturnEmptyOnNull() {
        Collection result = CollectionUtil.valuesFromSparseArray(null);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void valuesFromSparseArray_shouldReturnValuesOnNoTNull() {
        LongSparseArray<String> array = new LongSparseArray<>();
        array.put(1l, "1");
        array.put(5l, "2");
        array.put(43l, "3");
        array.put(192l, "4");
        Collection<String> result = CollectionUtil.valuesFromSparseArray(array);
        assertThat(result).containsOnly("1", "2", "3", "4");
    }
}
