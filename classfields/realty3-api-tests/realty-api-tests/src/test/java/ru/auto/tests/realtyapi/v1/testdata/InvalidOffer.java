package ru.auto.tests.realtyapi.v1.testdata;

public enum InvalidOffer {
    EMPTY("offers/empty.json");

    private final String value;

    InvalidOffer(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public String value() {
        return this.value;
    }
}
