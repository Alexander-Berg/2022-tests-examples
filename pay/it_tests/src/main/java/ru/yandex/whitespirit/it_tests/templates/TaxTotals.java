package ru.yandex.whitespirit.it_tests.templates;

import lombok.Value;


@Value
public class TaxTotals {
    TaxType taxType;
    double taxAmount;
}
