package ru.yandex.whitespirit.it_tests.templates;

import lombok.Value;

@Value
public class Firm {
    String inn;
    String name;
    String address;
    String representative;
    String kpp;
    String ogrn;
}
