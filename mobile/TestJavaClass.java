package ru.yandex.market.uikitapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.yandex.market.processor.testinstance.GenerateTestInstance;

public class TestJavaClass {

    @NonNull
    private final String s;

    @Nullable
    private final int i;

    @GenerateTestInstance
    public TestJavaClass(@NonNull final String s, final int i) {
        this.s = s;
        this.i = i;
    }
}
