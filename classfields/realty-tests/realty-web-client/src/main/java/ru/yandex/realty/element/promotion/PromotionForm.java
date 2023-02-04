package ru.yandex.realty.element.promotion;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.CheckboxWithLabel;

/**
 * Created by kopitsa on 07.08.17.
 */
public interface PromotionForm extends AtlasWebElement {

    @Name("Строка ввода данных «{{ value }}»")
    @FindBy(".//div[./div[text()='{{ value }}']]//span[@class = 'input__box']/input")
    AtlasWebElement row(@Param("value") String value);

    @Name("Чекбокс «{{ value }}»")
    @FindBy(".//span[./label/text()='{{ value }}']")
    CheckboxWithLabel checkbox(@Param("value") String value);

    @Name("Кнопка «Стать клиентом»")
    @FindBy(".//button[contains(@class, 'promotion__form-submit')]")
    AtlasWebElement submitButton();
}
