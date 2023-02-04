package ru.auto.tests.realtyapi.v1.testdata;

public enum Drafts {
    APARTMENT_SELL("draft/sell_draft.json"),
    APARTMENT_RENT("draft/rent_draft.json");

    private final String value;

    Drafts(String value) {
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
