package ru.yandex.whitespirit.it_tests.utils;

import io.restassured.http.Header;
import lombok.experimental.UtilityClass;
import one.util.streamex.StreamEx;
import ru.yandex.whitespirit.it_tests.templates.Firm;

import java.util.Map;
import java.util.function.Function;

@UtilityClass
public class Constants {
    public static final Firm HORNS_AND_HOOVES =  new Firm("100051000501", "ООО \\\"Рога и копыта\\\"",
            "Черноморск, ул. Приморская, 42",  "Зицпредседатель Фунт", "844245044", "5090865096240");
    public static final Firm HERKULES = new Firm("7704340310", "OOO \\\"Геркулес\\\"",
            "Черноморск, ул. Центральная, 1", "тов. Полыхаев", "645043055", "5127531383040");
    public static final Map<String, Firm> FIRMS = StreamEx.of(HORNS_AND_HOOVES, HERKULES)
            .toMap(Firm::getInn, Function.identity());
    public static final Header CONTENT_TYPE_HEADER = new Header("Content-Type", "application/json");
    public static final Header JSON_ACCEPT_HEADER = new Header("Accept", "application/json");
    public static final Header TEXT_PLAIN_ACCEPT_HEADER = new Header("Accept", "text/plain");
    public static final String OPENED_SHIFT_STATUS = "OPEN_SHIFT";
    public static final String CLOSED_SHIFT_STATUS = "CLOSE_SHIFT";
}
