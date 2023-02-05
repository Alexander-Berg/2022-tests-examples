package ru.yandex.navi;

public enum SearchCategory {
    DISCOUNTS("Скидки"),
    WHERE_TO_EAT("Где поесть"),
    YANDEX_GAS_STATIONS("Яндекс.Заправки");

    private final String value;

    SearchCategory(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
