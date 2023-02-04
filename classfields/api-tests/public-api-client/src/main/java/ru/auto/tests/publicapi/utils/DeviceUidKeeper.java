package ru.auto.tests.publicapi.utils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.Thread.currentThread;

public class DeviceUidKeeper {

    private Map<Long, Optional<Set<String>>> deviceUids = newHashMap();

    public void add(String deviceUid) {
        long threadId = currentThread().getId();

        if (deviceUids.containsKey(threadId)) {
            deviceUids.get(threadId).get().add(deviceUid);
        } else {
            deviceUids.put(threadId, Optional.of(newHashSet(deviceUid)));
        }
    }


    public Set<String> get() {
        long threadId = currentThread().getId();
        if (deviceUids.containsKey(threadId)) {
            return deviceUids.get(threadId).orElse(newHashSet());
        }

        return newHashSet();
    }

    public void clear() {
        long threadId = currentThread().getId();
        deviceUids.remove(threadId);
    }
}
