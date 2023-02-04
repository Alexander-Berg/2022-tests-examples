package ru.yandex.whitespirit.it_tests.configuration;

import lombok.Value;

import java.util.Map;

@Value
public class Config {
    boolean testExistingWhiteSpirit;
    boolean useLocalCompose;
    String whiteSpiritUrl;
    String hudsuckerUrl;
    Map<String, KKT> kkts;
}
