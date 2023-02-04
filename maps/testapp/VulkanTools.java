package com.yandex.maps.testapp;

import android.content.Context;
import android.content.SharedPreferences;

public class VulkanTools {
    private static final String MAP_SETTINGS_FILE = "map_settings";
    public static final String VULKAN_PREFERRED_KEY = "vulkan_preferred";

    public static Boolean readVulkanPreferred(final Context context) {
        SharedPreferences sharedPref = getSharedPreferences(context);
        return sharedPref.getBoolean(VULKAN_PREFERRED_KEY, false);
    }

    public static void storeVulkanPreferred(final Context context, final Boolean value) {
        SharedPreferences sharedPref = getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(VULKAN_PREFERRED_KEY, value);
        editor.commit();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(MAP_SETTINGS_FILE, Context.MODE_PRIVATE);
    }

    private VulkanTools() {
    }
}
