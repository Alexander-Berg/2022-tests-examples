package ru.auto.tests.realtyapi.adaptor.offer;

import lombok.Data;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

@Data
public class OfferTemplateData {
    public int price;

    public OfferTemplateData() {
        price = 400_000 + new Integer(randomNumeric(6));
    }
}
