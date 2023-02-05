package ru.yandex.mobile.avia.utils;

import android.util.Log;

import androidx.fragment.app.FragmentManager;

import com.orhanobut.logger.Logger;

import java.util.Collection;
import java.util.Map;

import ru.yandex.mobile.avia.BuildConfig;

public class TestLog {


    public static void debug(String tag, String message) {
        if (BuildConfig.TEST_ENV) {
            Logger.d(message);
        }
    }

    public static void debug(String message) {
        if (BuildConfig.TEST_ENV) {
            Logger.d(message);
        }
    }

    public static void debugStackTrace(String tag, String prefix, int level) {
        Log.d(tag, prefix + " stacktrace");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int length = Math.min(level, stackTrace.length);
        for (int i = 0; i < length; i++) {
            Log.d(tag, prefix + ": " + stackTrace[i]);
        }
    }

    public static void debugCollection(String tag, String prefix, Collection collection) {
        Log.d(tag, prefix + " collection");
        for (Object item : collection) {
            Log.d(tag, prefix + ": " + item);
        }
    }

    public static <K, V> void debugMap(String tag, String prefix, Map<K, V> map) {
        Log.d(tag, prefix + " map");
        for (Map.Entry<K, V> entry : map.entrySet()) {
            Log.d(tag, prefix + ": key:" + entry.getKey() + "; value: " + entry.getValue());
        }
    }

    public static void debugCurrentFragmentBackStack(final String tag, final FragmentManager manager) {
        int count = manager.getBackStackEntryCount();
        Log.e(tag, "onBackStackChanged <START>");
        for (int i = 0; i < count; i++) {
            Log.e(tag, "onBackStackChanged <" + i + "> : " + manager.getBackStackEntryAt(i).getName());
        }
        Log.e(tag, "onBackStackChanged <END>");
    }

    public static void debugFragmentBackStack(final String tag, final FragmentManager manager) {
        manager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                debugCurrentFragmentBackStack(tag, manager);
            }
        });

    }

}
