package ru.yandex.realty.consts;

/**
 * Created by vicdev on 31.05.17.
 */
public enum Location {
    RUSSIA("Россия", "/rossiya/"),
    SPB("Санкт-Петербург", "/sankt-peterburg/"),
    MOSCOW("Москва", "/moskva/"),
    SPB_AND_LO("Санкт-Петербург и ЛО", "/sankt-peterburg_i_leningradskaya_oblast/"),
    MOSCOW_AND_MO("Москва и МО", "/moskva_i_moskovskaya_oblast/"),
    MOSCOW_OBL("Московская область", "/moskovskaya_oblast/");

    String name;
    String path;

    Location(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }
    public String getPath() {
        return path;
    }

}
