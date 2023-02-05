package ru.yandex.disk.test;

import android.content.Intent;

import com.google.common.base.Predicate;

public class IntentActionEqualsPredicate implements Predicate<Intent> {
    private final String one;

    public IntentActionEqualsPredicate(String action) {
        one = action;
    }

    @Override
    public boolean apply(Intent intent) {
        String another = intent.getAction();
        if (one == another) {
            return true;
        }
        if (one != null && another == null || one == null && another != null) {
            return false;
        }
        //one and another are not null

        return one.equals(another);
    }

}
