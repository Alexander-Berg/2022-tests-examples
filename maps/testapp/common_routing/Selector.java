package com.yandex.maps.testapp.common_routing;

import org.jetbrains.annotations.NotNull;

import java.lang.IllegalStateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class Selector<T> {
    private List<T> items = Collections.emptyList();
    private Integer index;

    public void reset(@NotNull List<T> items) {
        select(null);
        this.items = new ArrayList<>(items);
    }

    public void selectFirst() throws IllegalStateException {
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot select first of none items");
        }
        select(0);
    }

    public void selectNext() {
        if (index != null && index + 1 != items.size()) {
            select(index + 1);
        }
    }

    public void selectPrev() {
        if (index != null && index != 0) {
            select(index - 1);
        }
    }

    public void selectCurrent() {
        if (index != null) {
            select(index);
        }
    }

    public List<T> getItems() {
        return Collections.unmodifiableList(items);
    }

    public T getCurrent() {
        if (index == null) {
            return null;
        } else {
            assert !items.isEmpty();
            return items.get(index);
        }
    }

    private void select(Integer index) {
        assert !items.isEmpty();
        if (this.index != null) {
            onDeselected(items.get(this.index), this.index);
        }
        this.index = index;
        if (this.index != null) {
            onSelected(items.get(this.index), this.index);
        }
    }

    protected abstract void onSelected(T item, int index);
    protected abstract void onDeselected(T item, int index);
}
