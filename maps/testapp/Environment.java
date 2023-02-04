package com.yandex.maps.testapp;

import android.content.Context;
import android.content.SharedPreferences;

import com.yandex.maps.testapp.mapdesign.HostConfig;
import com.yandex.maps.testapp.settings.SharedPreferencesConsts;

import java.util.HashMap;
import java.util.Map;

public class Environment {

    private final static Map<String, HostConfig> hostConfigs = new HashMap<String, HostConfig>() {{
            put("production", new HostConfig("core-renderer-stylerepo.maps.yandex.net",
            "core-renderer-cartograph.maps.yandex.net",
            "core-renderer-tiles.maps.yandex.net/vmap3mobile"));
            put("testing", new HostConfig("core-renderer-stylerepo.testing.maps.n.yandex.ru",
            "core-renderer-cartograph.testing.maps.n.yandex.ru",
            "core-renderer-cache.common.testing.maps.yandex.net/vmap3mobile"));
    }};

    public static HostConfig hostConfig(Context context) {
        final String env = readEnvironmentFromPreferences(context);
        return hostConfigs.get(env);
    }

    public static String readEnvironmentFromPreferences(final Context context) {
        SharedPreferences sPref = getEnvironmentPrefs(context);
        return sPref.getString(environmentKey(context), "production");
    }

    public static void writeEnvironmentToPreferences(final String environment, final Context context) {
        if (!hostConfigs.containsKey(environment))
            throw new RuntimeException("Not allowed environment value");

        SharedPreferences sPref = getEnvironmentPrefs(context);
        SharedPreferences.Editor editor = sPref.edit();
        editor.putString(environmentKey(context), environment);
        editor.apply();
    }

    private static SharedPreferences getEnvironmentPrefs(final Context context) {
        return context.getSharedPreferences(SharedPreferencesConsts.ENV_PREFS, Context.MODE_PRIVATE);
    }

    public static String environmentKey(final Context context) {
        return context.getString(R.string.environment);
    }
}
