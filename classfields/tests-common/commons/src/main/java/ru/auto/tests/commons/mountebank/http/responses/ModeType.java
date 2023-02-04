package ru.auto.tests.commons.mountebank.http.responses;

import lombok.Getter;

public enum ModeType {
    PROXY_ONCE("proxyOnce"),
    PROXY_ALWAYS("proxyAlways"),
    PROXY_TRANSPARENT("proxyTransparent");

    @Getter
    private String value;

    ModeType(String value) {

        this.value = value;
    }
}
