package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface Preview extends AtlasWebElement {

    String DELETE_ICON = "Удалить";
    String TURN_ICON = "Повернуть";

    @Name("Превью картинка")
    @FindBy(".//div[@class='preview__content']")
    AtlasWebElement previewImg();

    @Name("Кнопка «{{ value }}»")
    @FindBy(".//span[@aria-label='{{ value }}']")
    AtlasWebElement icon(@Param("value") String value);
}
