package com.yandex.maps.testapp.apiKeySettings;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class ApiKeyConsts {
    public static final String API_KEY_PREFS = "API_KEY_PREFERENCES";

    public static final String API_KEY_TYPE_KEY = "API_KEY_TYPE";

    public static final String YANDEX_STRING = "yandex";
    public static final String COMMERCIAL_STRING = "commercial";
    public static final String FREE_STRING = "free";

    public static final Map<String, String> API_KEYS_MAP;
    static {
        Map<String, String> temp = new HashMap<String, String>();
        temp.put(YANDEX_STRING, "05e62a59-735d-4f2c-a0bb-8ece97cc408f");
        temp.put(COMMERCIAL_STRING, "c0c0baf8-6754-4e31-8c24-004da3813830");
        temp.put(FREE_STRING, "d1f0d30d-20b8-4487-afc8-7021bc34cd50");
        API_KEYS_MAP = Collections.unmodifiableMap(temp);
    }
}
