package ru.auto.tests.publicapi.utils;

import java.util.Collections;
import java.util.List;

public class CollectionUtils {

    public static <T> List<T> emptyIfNull(List<T> list) {
        if (list == null) {
            return Collections.emptyList();
        } else {
            return list;
        }
    }
}
