package ru.yandex.intranet.d.web.controllers.front;

import ru.yandex.intranet.d.web.model.AmountDto;

public class AmountDtoBuilder {
    private String readableAmount;
    private String readableUnit;
    private String rawAmount;
    private String rawUnit;
    private String forEditAmount;
    private String forEditUnitId;
    private String amountInMinAllowedUnit;
    private String minAllowedUnit;

    public AmountDtoBuilder setReadableAmount(String readableAmount) {
        this.readableAmount = readableAmount;
        return this;
    }

    public AmountDtoBuilder setReadableUnit(String readableUnit) {
        this.readableUnit = readableUnit;
        return this;
    }

    public AmountDtoBuilder setRawAmount(String rawAmount) {
        this.rawAmount = rawAmount;
        return this;
    }

    public AmountDtoBuilder setRawUnit(String rawUnit) {
        this.rawUnit = rawUnit;
        return this;
    }

    public AmountDtoBuilder setForEditAmount(String forEditAmount) {
        this.forEditAmount = forEditAmount;
        return this;
    }

    public AmountDtoBuilder setForEditUnitId(String forEditUnitId) {
        this.forEditUnitId = forEditUnitId;
        return this;
    }

    public AmountDtoBuilder setAmountInMinAllowedUnit(String amountInMinAllowedUnit) {
        this.amountInMinAllowedUnit = amountInMinAllowedUnit;
        return this;
    }

    public AmountDtoBuilder setMinAllowedUnit(String minAllowedUnit) {
        this.minAllowedUnit = minAllowedUnit;
        return this;
    }

    public AmountDto build() {
        return new AmountDto(
                readableAmount,
                readableUnit,
                rawAmount,
                rawUnit,
                forEditAmount,
                forEditUnitId,
                amountInMinAllowedUnit,
                minAllowedUnit);
    }

    public AmountDtoBuilder setReadableAmount(String readableAmount, String readableUnit) {
        this.readableAmount = readableAmount;
        this.readableUnit = readableUnit;
        return this;
    }

    public AmountDtoBuilder setRawAmount(String rawAmount, String rawUnit) {
        this.rawAmount = rawAmount;
        this.rawUnit = rawUnit;
        return this;
    }

    public AmountDtoBuilder setForEditAmount(String forEditAmount, String forEditUnitId) {
        this.forEditAmount = forEditAmount;
        this.forEditUnitId = forEditUnitId;
        return this;
    }

    public AmountDtoBuilder setAmountInMinAllowedUnit(String amountInMinAllowedUnit, String minAllowedUnit) {
        this.amountInMinAllowedUnit = amountInMinAllowedUnit;
        this.minAllowedUnit = minAllowedUnit;
        return this;
    }
}
