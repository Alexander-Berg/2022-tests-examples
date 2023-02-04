package ru.yandex.arenda.account;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Thread.currentThread;

public class FlatsKeeper {

    private Map<Long, Optional<List<String>>> flats = newHashMap();

    public void add(String flat) {
        long threadId = currentThread().getId();
        if (flats.containsKey(threadId)) {
            flats.get(threadId).get().add(flat);
        } else {
            flats.put(threadId, Optional.of(newArrayList(flat)));
        }
    }

    public List<String> get() {
        long threadId = currentThread().getId();
        if (flats.containsKey(threadId)) {
            return flats.get(threadId).orElse(newArrayList());
        }
        return newArrayList();
    }

    public void clear() {
        long threadId = currentThread().getId();
        flats.remove(threadId);
    }
}
