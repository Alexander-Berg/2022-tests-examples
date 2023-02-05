package ru.yandex.maps.utils;

import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class CaptorUtils {
    private CaptorUtils() {
        throw new RuntimeException();
    }

    @SuppressWarnings("unchecked") // great power brings great responsibility
    public static <T> T listCaptor() {
        return (T) ArgumentCaptor.forClass(List.class);
    }

    @SuppressWarnings("unchecked") // great power brings great responsibility
    public static <T> T setCaptor() {
        return (T) ArgumentCaptor.forClass(Set.class);
    }

    @SuppressWarnings("unchecked") // great power brings great responsibility
    public static <T, S extends Collection> T collectionCaptor(Class<S> clazz) {
        return (T) ArgumentCaptor.forClass(clazz);
    }

    public static <T> ArgumentCaptor<T> captor(Class<T> clazz) {
        return ArgumentCaptor.forClass(clazz);
    }

}
