package ru.yandex.disk.utils;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.Uri;
import android.os.PatternMatcher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple intent 'map' which provide getting object by intent if it matches to
 * some intent filter.
 * 
 * @author feelgood
 * 
 * @param <T>
 */
public class IntentMatcher<T> extends HashMap<IntentFilter, T> {

    private static final String TAG = "IntentMatcher";

    public IntentMatcher() {
    }

    public T match(Intent intent) {
        for (Map.Entry<IntentFilter, T> entry : entrySet()) {
            IntentFilter filter = entry.getKey();
            if (match(filter, intent)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static boolean match(IntentFilter filter, Intent intent) {
        int match = filter.match(intent.getAction(), null, intent.getScheme(), intent.getData(), null, TAG);
        return match > 0;
    }

    public T matchExcludingData(Intent intent) {
        Intent clone = new Intent(intent);
        clone.setData(null);
        T value = match(clone);
        return value;
    }

    public void put(String action, T value) {
        IntentFilter filter = new IntentFilter(action);
        put(filter, value);
    }

    /**
     * 
     * @param action
     * @param data
     *            <i>base</i> URI for some of Content object from Contract, for
     *            instance: content://ru.yandex.mail/mail/
     * @param value
     * @throws MalformedMimeTypeException
     */
    public void put(String action, Uri data, T value) {
        IntentFilter filter = new IntentFilter(action);
        filter.addDataScheme(data.getScheme());
        filter.addDataAuthority(data.getHost(), data.getPort() > 0 ? String.valueOf(data.getPort()) : null);
        int type = PatternMatcher.PATTERN_PREFIX;
        filter.addDataPath(data.getPath(), type);
        put(filter, value);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }

        StringBuilder buffer = new StringBuilder(size() * 28);
        buffer.append('{');
        Iterator<Map.Entry<IntentFilter, T>> it = entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<IntentFilter, T> entry = it.next();
            Object key = entry.getKey();
            if (key != this) {
                buffer.append(toString((IntentFilter) key));
            } else {
                buffer.append("(this Map)");
            }
            buffer.append('=');
            Object value = entry.getValue();
            if (value != this) {
                buffer.append(value);
            } else {
                buffer.append("(this Map)");
            }
            if (it.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    private String toString(IntentFilter filter) {
        StringBuilder buffer = new StringBuilder();
        buffer.append('\n');
        appendIterator("actions", filter.actionsIterator(), buffer);
        appendIterator("types", filter.typesIterator(), buffer);
        appendIterator("schemes", filter.schemesIterator(), buffer);
        appendIterator("authorities", filter.authoritiesIterator(), buffer);
        appendIterator("paths", filter.pathsIterator(), buffer);
        return buffer.toString();
    }

    private void appendIterator(String propertyName, Iterator<?> it, StringBuilder buffer) {
        if (it != null && it.hasNext()) {
            buffer.append(propertyName).append(" [");
            while (it.hasNext()) {
                buffer.append(it.next());
                if (it.hasNext()) {
                    buffer.append(", ");
                }
            }
            buffer.append("]");
        }
    }

}
