package ru.yandex.whitespirit.it_tests.templates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Group {
    DEFAULT("_NOGROUP"),
    RZHD("RZHD");

    private final String value;
}
