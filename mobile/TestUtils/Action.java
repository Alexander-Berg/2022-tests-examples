package ru.yandex.direct.ui.testutils;

import androidx.annotation.Nullable;

public interface Action<T1, T2> {

    void invoke(@Nullable T1 first, @Nullable T2 second);

}
