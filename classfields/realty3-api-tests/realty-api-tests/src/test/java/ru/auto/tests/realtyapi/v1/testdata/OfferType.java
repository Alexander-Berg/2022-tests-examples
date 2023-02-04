package ru.auto.tests.realtyapi.v1.testdata;

public enum OfferType {
    //VERTISTEST-654
    APARTMENT_SELL("offers/apartment_sell.ftl"),
    APARTMENT_RENT("offers/apartment_rent.json"),
    HOUSE_SELL("offers/house_sell.ftl"),
    HOUSE_RENT("offers/house_rent.json"),
    ROOM_SELL("offers/room_sell.ftl"),
    ROOM_RENT("offers/room_rent.json"),
    LOT_SELL("offers/lot_sell.ftl");

    private final String value;

    OfferType(String value) {
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
