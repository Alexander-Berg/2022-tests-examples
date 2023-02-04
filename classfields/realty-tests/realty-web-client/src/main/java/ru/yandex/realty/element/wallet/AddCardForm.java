package ru.yandex.realty.element.wallet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface AddCardForm extends AtlasWebElement {

    @Name("Номер карты")
    @FindBy(".//*[@placeholder='Номер карты']")
    AtlasWebElement cardNumber();

    @Name("Активна до «Месяц»")
    @FindBy(".//*[@placeholder='ММ']")
    AtlasWebElement month();

    @Name("Активна до «Год»")
    @FindBy(".//*[@placeholder='ГГ']")
    AtlasWebElement year();

    @Name("CVC")
    @FindBy(".//*[@placeholder='CVC']")
    AtlasWebElement cardCvc();
}
