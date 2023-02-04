package ru.yandex.whitespirit.it_tests.templates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaxType {
    NDS_18(20),
    NDS_20(20),
    NDS_10(10),
    NDS_0(0),
    NDS_NONE(0),
    NDS_18_118(20),
    NDS_10_110(10),
    NDS_20_120(20);

    private final int pct;
}
