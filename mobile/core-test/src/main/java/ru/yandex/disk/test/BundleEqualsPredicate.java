package ru.yandex.disk.test;

import android.os.Bundle;

import com.google.common.base.Predicate;

public class BundleEqualsPredicate implements Predicate<Bundle> {

    private final Bundle actual;

    public BundleEqualsPredicate(Bundle actual) {
        this.actual = actual;
    }

    @Override
    public boolean apply(Bundle expected) {
        if (expected == null && actual == null) {
            return true;
        }
        if (expected == null) {
            return false;
        }

        if (actual == null) {
            return false;
        }

        if (expected.keySet().equals(actual.keySet())) {
            for (String key : expected.keySet()) {
                if (!expected.get(key).equals(actual.get(key))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
