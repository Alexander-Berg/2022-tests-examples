package com.yandex.mail.react.model;

import android.content.Context;

import androidx.annotation.NonNull;

public class TestLinkUnwrapper extends LinkUnwrapper {

    private boolean isYaBroDefault;

    public TestLinkUnwrapper(@NonNull Context context, long uid) {
        super(context, uid);
    }

    public void setYaBroDefault(boolean yaBroDefault) {
        isYaBroDefault = yaBroDefault;
    }

    @Override
    protected boolean isYaBroDefault(@NonNull String link) {
        return isYaBroDefault;
    }
}
