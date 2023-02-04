package ru.yandex.vertis.bean;


import com.github.tomakehurst.wiremock.junit.WireMockRule;

import java.util.HashMap;
import java.util.Map;

public class Config {

    public static Map<String, String> tcConfig(WireMockRule wireMockRule) {
        Map<String, String> tcConfig = new HashMap<>();
        tcConfig.put("teamcity.serverUrl", wireMockRule.baseUrl());
        tcConfig.put("rerun.test.build", ".lastFinished");
        return tcConfig;
    }

    public static Map<String, String> tcConfig() {
        Map<String, String> tcConfig = new HashMap<>();
        tcConfig.put("rerun.test.build", ".lastFinished");
        return tcConfig;
    }

    public static Map<String, String> buildConfig() {
        Map<String, String> tcConfig = new HashMap<>();
        tcConfig.put("teamcity.auth.userId", "123");
        tcConfig.put("teamcity.auth.password", "qwe");
        return tcConfig;
    }

}
