package ru.yandex.realty.element.offers;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.saleads.InputField;

/**
 * Created by vicdev on 08.06.17.
 */
public interface PriceField extends InnerContent, InputField {
    @Name("Блок со свойством \"{{ value }}\"")
    @FindBy("//div[contains(@class, 'offer-form-row') and " +
            "(.//div[contains(@class, 'offer-form-row__title')]/text()='{{ value }}')]")
    FeatureField featureField(@Param("value") String value);

    @Name("Инпут цены")
    @FindBy(".//input[@id='input-price']")
    AtlasWebElement priceInput();

    @Name("Поле «Предоплата, %»")
    @FindBy(".//div[contains(@class,'offer-form-field_name_prepayment')]//input")
    AtlasWebElement prePayment();

    @Name("Поле «Коммисия агента, %»")
    @FindBy(".//div[contains(@class,'offer-form-field_name_agentFee')]//input")
    AtlasWebElement agentFee();

    @Name("Поле «Обеспечительный платёж, %»")
    @FindBy(".//div[contains(@class,'offer-form-field_name_securityPayment')]//input")
    AtlasWebElement securityPayment();
}

