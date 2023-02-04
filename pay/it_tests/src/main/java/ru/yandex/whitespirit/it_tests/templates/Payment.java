package ru.yandex.whitespirit.it_tests.templates;

import lombok.Value;

@Value
public class Payment {
    double amount;
    PaymentType type;
}
