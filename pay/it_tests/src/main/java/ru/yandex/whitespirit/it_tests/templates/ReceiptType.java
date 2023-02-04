package ru.yandex.whitespirit.it_tests.templates;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ReceiptType {
    INCOME(1),
    RETURN_INCOME(2),
    CHARGE(3),
    RETURN_CHARGE(4);

    private final int code;
}
