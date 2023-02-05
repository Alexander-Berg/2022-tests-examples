package com.yandex.mail.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class MergedReadOnlyIterableTest {

    @Test
    public void mergedIterable_forEachOk() {
        final int size = 5;
        List<Integer> list1 = new ArrayList<>(size);
        List<Integer> list2 = new ArrayList<>(size);
        List<Integer> list3 = new ArrayList<>(size);

        fillList(size, list1);
        fillList(size, list2);
        fillList(size, list3);

        Set<List<Integer>> merged = new HashSet<>(3);
        merged.add(list1);
        merged.add(list2);
        merged.add(list3);

        List<Integer> loopResult = new ArrayList<>(size * 3);
        for (List<Integer> list : merged) {
            loopResult.addAll(list);
        }

        int i = 0;
        for (Integer fromIterable : new MergedReadOnlyIterable<>(merged)) {
            assertThat(loopResult.get(i++)).isEqualTo(fromIterable);
        }
    }

    @Test
    public void mergedIterable_forEachOkWithEmptyIterable() {
        final int size = 5;
        List<Integer> list1 = new ArrayList<>(size);
        List<Integer> list2 = new ArrayList<>(size);
        List<Integer> list3 = new ArrayList<>(size);

        fillList(size, list1);
        fillList(size, list3);

        Set<List<Integer>> merged = new HashSet<>(3);
        merged.add(list1);
        merged.add(list2);
        merged.add(list3);

        List<Integer> loopResult = new ArrayList<>(size * 3);
        for (List<Integer> list : merged) {
            loopResult.addAll(list);
        }

        int i = 0;
        for (Integer fromIterable : new MergedReadOnlyIterable<>(merged)) {
            assertThat(loopResult.get(i++)).isEqualTo(fromIterable);
        }
    }

    @Test
    public void mergedIterable_doesNotSupportRemove() {
        final int size = 5;
        List<Integer> list1 = new ArrayList<>(size);
        List<Integer> list2 = new ArrayList<>(size);

        Set<List<Integer>> set = new HashSet<>();
        set.add(list1);
        set.add(list2);

        Iterable<Integer> mergedReadOnlyIterable = new MergedReadOnlyIterable<>(set);
        Iterator<Integer> it = mergedReadOnlyIterable.iterator();
        try {
            it.remove();
            failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
        } catch (UnsupportedOperationException e) {
            //no op it's ok
        }
    }

    private void fillList(int size, @NonNull List<Integer> list) {
        while (size > 0) {
            list.add(size--);
        }
    }
}
