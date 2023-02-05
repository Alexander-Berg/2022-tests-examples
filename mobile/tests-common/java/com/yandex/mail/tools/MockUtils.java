package com.yandex.mail.tools;

import com.yandex.mail.util.NonInstantiableException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import androidx.annotation.NonNull;
import timber.log.Timber;

public final class MockUtils {

    private MockUtils() {
        throw new NonInstantiableException();
    }

    /**
     * @return Dummy instance of {@code clazz}.
     */
    @NonNull
    public static <T> T makeMock(@NonNull Class<T> clazz, @NonNull Object... params) {
        try {
            Class[] classes = new Class[params.length];
            for (int i = 0; i < params.length; i++) {
                classes[i] = params[i].getClass();
            }
            final Constructor<T> constructor = clazz.getDeclaredConstructor(classes);
            constructor.setAccessible(true);
            return constructor.newInstance(params);
            // we have to keep these catch clauses split due to API < 19
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            Timber.e(e);
            return null;
        }
    }
}
