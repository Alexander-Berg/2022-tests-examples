package ru.auto.tests.publicapi.adaptor.offer;

import lombok.Data;

@Data
public class OfferTemplateData {
    public String phone;
    public String licensePlate;
    public String vin;

    public OfferTemplateData withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public OfferTemplateData withLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
        return this;
    }

    public OfferTemplateData withVin(String vin) {
        this.vin = vin;
        return this;
    }
}
