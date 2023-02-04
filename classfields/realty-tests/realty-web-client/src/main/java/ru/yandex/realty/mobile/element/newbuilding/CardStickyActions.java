package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.mobile.element.Link;

public interface CardStickyActions extends Link {

    @Name("Время работы")
    @FindBy(".//div[@class='CardPhone__call-time']")
    AtlasWebElement workTime();

    @Name("Иконка подсказки")
    @FindBy(".//span[contains(@class,'CardPhone__hint')]")
    AtlasWebElement hint();
}
