package com.yandex.launcher.common.history.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;

import com.yandex.launcher.common.util.JsonDiskStorage;
import com.yandex.launcher.common.util.Logger;
import com.yandex.launcher.common.util.TextUtils;
import com.yandex.launcher.common.util.Thunk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;

/**
 * Old implementation of history.
 * Will be used for compatibility tests for a while
 */
@Deprecated
class UserHistory {

    @SuppressWarnings("WeakerAccess")
    static class HistoryEntry {
        public final String key;
        public String value;
        public float[] trVisits;
        public float rating;
        public long lastUsageTime;

        @Thunk
        HistoryEntry(String key, String value, int numberOfDays, long lastUsageTime) {
            this.key = key;
            this.value = value;
            this.trVisits = new float[numberOfDays];
            this.lastUsageTime = lastUsageTime;
            for (int i = 0; i < numberOfDays; ++i) {
                trVisits[i] = transformVisits(0.f);
            }
        }

        @Thunk
        HistoryEntry(HistoryEntry o){
            this.key = o.key;
            this.value = o.value;
            this.rating = o.rating;
            this.trVisits = new float[o.trVisits.length];
            this.lastUsageTime = o.lastUsageTime;
            System.arraycopy(o.trVisits, 0, trVisits, 0, o.trVisits.length);
        }

        @Thunk
        HistoryEntry(String key, String value, float[] trVisits, long lastUsageTime) {
            this.key = key;
            this.value = value;
            this.trVisits = trVisits;
            this.lastUsageTime = lastUsageTime;
        }
    }

    public static class Snapshot {
        @Thunk
        long lastUpdateDay;
        @Nullable
        @Thunk
        HistoryEntry[] sorted = null;
    }

    private final static String TAG = "UserHistory";
    private final static Logger logger = Logger.createInstance(TAG);
    private final static long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);
    private final static int MULTIPLIER = 10000;

    private final static int DATA_VERSION = 1;
    private final static String JSON_VERSION = "version";
    private final static String JSON_LAST_UPDATE_DAY = "last_update_day";
    private final static String JSON_ENTRIES = "entries";
    private final static String JSON_KEY = "key";
    private final static String JSON_VALUE = "value";
    private final static String JSON_VISITS = "visits";
    private final static String JSON_LAST_USAGE = "last_usage";

    private final String id;
    private final int numberOfDays;
    private final int maxNumberOfObjects;
    private long  lastUpdateDay;
    private final SimpleArrayMap<String, HistoryEntry> map = new SimpleArrayMap<>();
    private final ArrayList<HistoryEntry> sorted = new ArrayList<>();
    private final Object dataLock = new Object();
    private final JsonDiskStorage<SavedData> jsonDiskStorage;

    public UserHistory(Context context, String id, int numberOfDays) {
        this(context, id, numberOfDays, -1);
    }

    public UserHistory(Context context, String id, int numberOfDays, int maxNumberOfObjects) {
        this.id = id;
        this.numberOfDays = numberOfDays;
        this.maxNumberOfObjects = maxNumberOfObjects;
        this.jsonDiskStorage = new JsonDiskStorage<>(context, TextUtils.engFormat("user-history-%s", id), createDataHost());
    }

    public void load() {
        logger.d("load (%s)", id);

        SavedData savedData = jsonDiskStorage.load();

        if (savedData == null) {
            return;
        }

        synchronized (dataLock) {
            sorted.clear();
            map.clear();
            this.lastUpdateDay = savedData.lastUpdateDay;
            for (HistoryEntry e : savedData.entries) {
                if (!map.containsKey(e.key)) {
                    map.put(e.key, e);
                    sorted.add(e);
                } else {
                    logger.e("onDataLoaded - duplicated keys");
                }
            }
            shiftHistoryLocked();
            for (HistoryEntry e : savedData.entries) {
                calculateRatingLocked(e);
            }
            Collections.sort(sorted, (a, b) -> a.rating < b.rating ? 1 :
                    a.rating > b.rating ? -1 : 0);

            if (maxNumberOfObjects > 0) {
                while (sorted.size() > maxNumberOfObjects) {
                    removeTheLeastPopularObjectLocked();
                }
            }
        }
    }

    public boolean isEmpty() {
        synchronized (dataLock) {
            return sorted.isEmpty();
        }
    }

    public boolean contains(String key) {
        synchronized (dataLock) {
            return map.containsKey(key);
        }
    }

    public String get(String key) {
        synchronized (dataLock) {
            HistoryEntry e = map.get(key);
            return e != null ? e.value : null;
        }
    }

    public float getRating(String key) {
        synchronized (dataLock) {
            final HistoryEntry entry = map.get(key);
            if (entry != null) {
                return entry.rating;
            }
        }
        return 0f;
    }

    public ArrayList<String> getTop(int max) {
        synchronized (dataLock) {
            int historySize = sorted.size();
            int size = max >= 0 ? Math.min(historySize, max) : historySize;
            ArrayList<String> result = new ArrayList<>();
            for (int i = 0; i < size; ++i) {
                result.add(sorted.get(i).key);
            }
            return result;
        }
    }

    public ArrayList<String> getLast(int max) {
        return getByComparator(max, (a, b) -> a.lastUsageTime == b.lastUsageTime ? 0 :
                a.lastUsageTime > b.lastUsageTime ? -1 : 1);
    }

    private ArrayList<String> getByComparator(int max, Comparator<HistoryEntry> comparator) {
        synchronized (dataLock) {
            final int historySize = sorted.size();
            ArrayList<HistoryEntry> sortedByRecentlyUsed = new ArrayList<>(sorted);
            Collections.sort(sortedByRecentlyUsed, comparator);
            final int size = max >= 0 ? Math.min(historySize, max) : historySize;
            final ArrayList<String> result = new ArrayList<>();
            result.ensureCapacity(size);
            for (int i = 0; i < size; ++i) {
                result.add(sortedByRecentlyUsed.get(i).key);
            }
            return result;
        }
    }

    public void clear() {
        logger.d("clear (%s)", id);
        synchronized (dataLock) {
            map.clear();
            sorted.clear();
        }
        jsonDiskStorage.postSave();
    }

    public void flush() {
        logger.d("flush (%s)", id);
        jsonDiskStorage.flush();
    }

    @SuppressLint("DefaultLocale")
    public void dump(String tag) {
        logger.d("dump %s >>>> ", tag);
        synchronized (dataLock) {
            for (HistoryEntry e : sorted) {
                logger.d(String.format("    %s, %.4f: %s -> %s", tag, e.rating, e.key, e.value));
            }
        }
        logger.d("dump %s <<<< ", tag);
    }

    public void add(String key) {
        add(key, null);
    }

    public void add(String key, String value) {
        add(key, value, System.currentTimeMillis(), 1.f);
    }

    public void add(String key, long time, float visits) {
        add(key, null, time, visits);
    }

    public void add(String key, String value, long time, float visits) {
        logger.d("add (%s) key=%s", id, key);
        synchronized (dataLock) {
            shiftHistoryLocked();
            addLocked(key, value, time, visits, false);
        }
        jsonDiskStorage.postSave();
    }

    /**
     * Rewrites value in Entry object
     */
    public void put(String key, String value) {
        logger.d("add (%s) key=%s", id, key);
        synchronized (dataLock) {
            shiftHistoryLocked();
            addLocked(key, value, System.currentTimeMillis(), 1.f, true);
        }
        jsonDiskStorage.postSave();
    }

    private void addLocked(String key, String value, long time, float visits, boolean forceRewriteValue) {
        if (key == null || key.isEmpty()) {
            return;
        }
        HistoryEntry entry = map.get(key);
        if (entry == null) {
            if (maxNumberOfObjects > 0 && map.size() >= maxNumberOfObjects) {
                removeTheLeastPopularObjectLocked();
            }
            entry = new HistoryEntry(key, value, numberOfDays, time);
            map.put(key, entry);
            sorted.add(entry);
        } else if (forceRewriteValue) {
            entry.value = value;
        }
        entry.lastUsageTime = time;
        long currentDay = getDay(System.currentTimeMillis());
        long eventDay = getDay(time);
        int relativeDayIndex = (int) (currentDay - eventDay);
        if (relativeDayIndex >= 0 && relativeDayIndex < entry.trVisits.length) {
            addVisitsLocked(entry, relativeDayIndex, visits);
            calculateRatingLocked(entry);
            reorderEntryLocked(entry);
        }
    }

    public String remove(String key) {
        logger.d("remove (%s) key=%s", id, key);
        boolean removed = false;
        String value = null;
        synchronized (dataLock) {
            HistoryEntry entry = map.remove(key);
            if (entry != null) {
                sorted.remove(entry);
                removed = true;
                value = entry.value;
            }
        }
        if (removed) {
            jsonDiskStorage.postSave();
        }
        return value;
    }

    public List<String> getKeys() {
        List<String> result = new ArrayList<>();
        synchronized (dataLock) {
            for (int i = 0; i < map.size(); i++) {
                result.add(map.keyAt(i));
            }
        }
        return result;
    }

    public Snapshot createSnapshot() {
        Snapshot snapshot = new Snapshot();
        synchronized (dataLock) {
            snapshot.lastUpdateDay = lastUpdateDay;
            snapshot.sorted = new HistoryEntry[sorted.size()];
            for (int i = 0; i < snapshot.sorted.length; i++) {
                snapshot.sorted[i] = new HistoryEntry(sorted.get(i));
            }
        }

        return snapshot;
    }

    public void restoreSnapshot(Snapshot snapshot) {
        logger.d("restoreSnaphot (%s)", id);
        synchronized (dataLock) {
            map.clear();
            sorted.clear();

            if (snapshot.sorted != null) {
                Collections.addAll(sorted, snapshot.sorted);

                for (HistoryEntry e : snapshot.sorted) {
                    map.put(e.key, e);
                }
            }
            lastUpdateDay = snapshot.lastUpdateDay;
        }
        jsonDiskStorage.postSave();
    }

    @Thunk
    static float transformVisits(float visits) {
        return (float) Math.log(1.f + visits);
    }

    private static void addVisitsLocked(HistoryEntry e, int index, float addition) {
        double visits = (Math.exp(e.trVisits[index]));
        e.trVisits[index] = (float) Math.log(visits + addition);
    }

    private static long getDay(long time) {
        return time / MILLIS_PER_DAY;
    }

    private static void calculateRatingLocked(HistoryEntry entry) {
        float rating = 0.f;

        for (int i = 0; i < entry.trVisits.length; ++i) {
            float trVisits = entry.trVisits[i];
            rating += trVisits * (21.f + i) / (7.f + i);
        }
        entry.rating = rating;
    }

    private void reorderEntryLocked(HistoryEntry entry) {
        int index = sorted.indexOf(entry);
        for (int i = index - 1; i >= 0; --i) {
            if (sorted.get(i).rating >= entry.rating) {
                break;
            }
            sorted.set(i + 1, sorted.get(i));
            sorted.set(i, entry);
        }
    }

    private void shiftHistoryLocked() {
        long currentDay = getDay(System.currentTimeMillis());
        if (lastUpdateDay != 0) {
            if (currentDay != lastUpdateDay) {
                shiftHistoryLockedImpl((int) (currentDay - lastUpdateDay));
            }
        }
        lastUpdateDay = currentDay;
    }

    private void shiftHistoryLockedImpl(int val) {
        if (val <= 0) {
            return;
        }
        for (HistoryEntry entry : sorted) {
            for (int i = entry.trVisits.length - 1; i >= 0; --i) {
                if (i - val >= 0) {
                    entry.trVisits[i] = entry.trVisits[i - val];
                } else {
                    entry.trVisits[i] = 0.f;
                }
            }
        }
    }

    private void removeTheLeastPopularObjectLocked() {
        HistoryEntry entryToRemove = sorted.remove(sorted.size() - 1);
        map.remove(entryToRemove.key);
    }

    // Save/load data

    private static class SavedData {
        final ArrayList<HistoryEntry> entries;
        final long lastUpdateDay;

        @Thunk
        SavedData(ArrayList<HistoryEntry> entries, long lastUpdateDay) {
            this.entries = entries;
            this.lastUpdateDay = lastUpdateDay;
        }
    }

    private JsonDiskStorage.DataHost<SavedData> createDataHost() {
        return new JsonDiskStorage.DataHost<SavedData>() {
            @Override
            public void saveData(JsonWriter jsonWriter) throws IOException {
                saveDataImpl(jsonWriter);
            }

            @Override
            public SavedData parseData(JsonReader jsonReader) throws IOException {
                return parseDataImpl(jsonReader);
            }
        };
    }

    @Thunk
    SavedData parseDataImpl(JsonReader jsonReader) throws IOException {
        int parsedVersion = -1;
        long parsedLastUpdateDay = -1;
        ArrayList<HistoryEntry> parsedEntries = null;

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String attrName = jsonReader.nextName();
            switch (attrName) {
                case JSON_LAST_UPDATE_DAY:
                    parsedLastUpdateDay = jsonReader.nextLong();
                    break;
                case JSON_VERSION:
                    parsedVersion = jsonReader.nextInt();
                    break;
                case JSON_ENTRIES:
                    parsedEntries = parseEntries(jsonReader);
                    break;
                default:
                    jsonReader.skipValue();
                    break;
            }
        }
        jsonReader.endObject();

        if (parsedVersion < 0 || parsedLastUpdateDay < 0 || parsedEntries == null) {
            return null;
        }

        return new SavedData(parsedEntries, parsedLastUpdateDay);
    }

    private ArrayList<HistoryEntry> parseEntries(JsonReader jsonReader) throws IOException {
        logger.d("parseEntries (%s)", id);
        ArrayList<HistoryEntry> entries = new ArrayList<>();

        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            String key = null;
            String value = null;
            float[] visits = null;
            long lastUsage = 0;
            while (jsonReader.hasNext()) {
                String attrName = jsonReader.nextName();
                switch (attrName) {
                    case JSON_KEY:
                        key = jsonReader.nextString();
                        break;
                    case JSON_VALUE:
                        if (jsonReader.peek() != JsonToken.NULL) {
                            value = jsonReader.nextString();
                        } else {
                            jsonReader.nextNull();
                        }
                        break;
                    case JSON_VISITS:
                        visits = parseVisits(jsonReader);
                        break;
                    case JSON_LAST_USAGE:
                        lastUsage = jsonReader.nextLong();
                        break;
                    default:
                        jsonReader.skipValue();
                        break;
                }
            }
            jsonReader.endObject();
            if (key == null || visits == null) {
                logger.e("Invalid entry format");
                continue;
            }
            entries.add(new HistoryEntry(key, value, visits, lastUsage));
        }
        jsonReader.endArray();
        return entries;
    }

    private float[] parseVisits(JsonReader jsonReader) throws IOException {
        float[] visits = new float[numberOfDays];

        int index = 0;
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            int dayVisits = jsonReader.nextInt();
            if (index < numberOfDays) {
                visits[index++] = (float)dayVisits / MULTIPLIER;
            }
        }
        jsonReader.endArray();
        return visits;
    }

    @Thunk
    void saveDataImpl(JsonWriter jsonWriter) throws IOException {
        logger.d("saveDataImpl (%s)", id);
        Snapshot snapshot = createSnapshot();

        jsonWriter.beginObject();
        jsonWriter.name(JSON_VERSION).value(DATA_VERSION);
        jsonWriter.name(JSON_LAST_UPDATE_DAY).value(snapshot.lastUpdateDay);
        jsonWriter.name(JSON_ENTRIES).beginArray();

        if (snapshot.sorted != null) {
            for (HistoryEntry entry : snapshot.sorted) {
                if (entry.key == null || entry.key.isEmpty()) {
                    continue;
                }
                jsonWriter.beginObject();
                jsonWriter.name(JSON_KEY).value(entry.key);
                jsonWriter.name(JSON_VALUE).value(entry.value);
                jsonWriter.name(JSON_LAST_USAGE).value(entry.lastUsageTime);
                jsonWriter.name(JSON_VISITS).beginArray();
                for (float v : entry.trVisits) {
                    jsonWriter.value((int) (v * MULTIPLIER));
                }
                jsonWriter.endArray();
                jsonWriter.endObject();
            }
        }
        jsonWriter.endArray();
        jsonWriter.endObject();
    }
}
