package ru.yandex.qe.cache;

import javax.annotation.Nullable;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 05.07.13
 * Time: 21:02
 */
public interface TestStorage {

    @Nullable
    Object getData(int id);

    void put(int id, Object object);
}
