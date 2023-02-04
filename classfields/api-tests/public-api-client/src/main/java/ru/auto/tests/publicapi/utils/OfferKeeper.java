package ru.auto.tests.publicapi.utils;

import ru.auto.tests.publicapi.model.AutoApiOffer;

import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Thread.currentThread;

public class OfferKeeper {

    private Map<Long, Optional<Map<String, AutoApiOffer.CategoryEnum>>> offers = newHashMap();

    public void add(String offerId, AutoApiOffer.CategoryEnum category) {
        long threadId = currentThread().getId();
        if (offers.containsKey(threadId)) {
            offers.get(threadId).get().put(offerId, category);
        } else {
            Map<String, AutoApiOffer.CategoryEnum> map = newHashMap();
            map.put(offerId, category);
            offers.put(threadId, Optional.of(map));
        }
    }

    public Map<String, AutoApiOffer.CategoryEnum> get() {
        long threadId = currentThread().getId();
        if (offers.containsKey(threadId)) {
            return offers.get(threadId).orElse(newHashMap());
        }

        return newHashMap();
    }

    public void clear() {
        long threadId = currentThread().getId();
        offers.remove(threadId);
    }
}
