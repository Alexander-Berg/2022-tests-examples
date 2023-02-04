package ru.yandex.whitespirit.it_tests.templates;

import lombok.Value;
import lombok.With;

@With
@Value
public class Row {
    double price;
    double qty;
    TaxType taxType;
    PaymentTypeType paymentTypeType;

    public double getAmount() {
        return price * qty;
    }

    public Double getTaxAmount() {
        return (getAmount() * taxType.getPct()) / (100.0d + taxType.getPct());
    }
}
