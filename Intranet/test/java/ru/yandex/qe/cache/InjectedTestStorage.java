package ru.yandex.qe.cache;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA.
 * User: terry
 * Date: 05.08.13
 * Time: 21:36
 */
@Component
public class InjectedTestStorage extends BaseTestStorage {

    @Inject
    @Named("injectedCache")
    private Ehcache cache;

    @Nullable
    @Override
    public Object getData(int id) {
        Element element = cache.get(id);
        if (element == null) {
            cache.put(element = new Element(id, keyValue.get(id)));
        }
        return element.getObjectValue();
    }

    @Override
    public void put(int id, Object object) {
        keyValue.put(id, object);
        cache.removeAll();
    }
}
