package ru.auto.tests.realtyapi.v2.testdata;

import java.util.Arrays;
import java.util.List;

import static ru.auto.tests.commons.util.Utils.getRandomString;

public class FlatPlansTestData {
    private FlatPlansTestData() {}

    public static List<String> addressesForFlatPlans() {
        return Arrays.asList(
                "Россия, Санкт-Петербург, Невский проспект, 128",
                "Россия, Кемерово, проспект Ленина, 82",
                "Россия, Приморский край, Владивосток, проспект Острякова, 6",
                getRandomString()
        );
    }
}
