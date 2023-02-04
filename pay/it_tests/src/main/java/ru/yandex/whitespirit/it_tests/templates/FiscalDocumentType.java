package ru.yandex.whitespirit.it_tests.templates;

public enum FiscalDocumentType {
    RECEIPT("Receipt"),
    BSO("BSO");

    private final String str;

    FiscalDocumentType(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
