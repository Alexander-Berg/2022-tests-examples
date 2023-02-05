package ru.yandex.disk.test;

import android.content.Intent;

import com.google.common.base.Predicate;

public class ContextActionRequestPredicate implements Predicate<ContextActionRequest> {

    private final Predicate<Intent> predicate;

    public ContextActionRequestPredicate(Predicate<Intent> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean apply(ContextActionRequest contextActionRequest) {
        return predicate.apply(contextActionRequest.getIntent());
    }

}
