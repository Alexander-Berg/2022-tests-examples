package ru.yandex.qe.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 05.08.13
 * Time: 21:41
 */
public abstract  class BaseTestStorage implements TestStorage {
    protected Map keyValue = new HashMap();

    public void setKeyValue(Map keyValue) {
        this.keyValue = keyValue;
    }
}
