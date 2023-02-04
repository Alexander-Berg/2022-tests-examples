package ru.yandex.qe.bus.server;

import java.util.Iterator;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;

import ru.yandex.qe.bus.api.ApiJsonObject;
import ru.yandex.qe.bus.api.ApiService;

/**
 * Established by terry
 * on 30.01.14.
 */
@Service("apiService")
public class ApiImplService implements ApiService {

    @Nonnull
    public String getLogin(String login) {
        return login;
    }

    @Nonnull
    @Override
    public Iterator<ApiJsonObject> getLotOfObjects(final long numberOfObjects) {
        return new Iterator<ApiJsonObject>() {
            private long counter = 0;

            @Override
            public boolean hasNext() {
                return counter < numberOfObjects;
            }

            @Override
            public ApiJsonObject next() {
                counter++;
                return new ApiJsonObject(counter, String.valueOf(counter));
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Iterator<String> transformLofOfObjects(final Iterator<String> object) {
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return object.hasNext();
            }

            @Override
            public String next() {
                return object.next() + " from server";
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int putLotOfObject(Iterator<ApiJsonObject> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            try {
                iterator.next();
            } catch (RuntimeException e) {
                throw e;
            }
            count++;
        }
        return count;
    }
}
