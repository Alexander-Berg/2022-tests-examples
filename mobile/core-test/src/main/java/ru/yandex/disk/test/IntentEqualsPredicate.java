package ru.yandex.disk.test;

import android.content.Intent;

import com.google.common.base.Predicate;

public class IntentEqualsPredicate implements Predicate<Intent> {
    private final Intent one;

    public IntentEqualsPredicate(Intent intent) {
        one = intent;
    }

    @Override
    public boolean apply(Intent another) {
        if (one == another) {
            return true;
        }
        if (one != null && another == null || one == null && another != null) {
            return false;
        }
        //one and another are not null

        if (!one.filterEquals(another)) {
            return false;
        }

        return new BundleEqualsPredicate(one.getExtras()).apply(another.getExtras());
    }

}
