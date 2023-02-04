package ru.auto.tests.desktop.element.cabinet.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 19.04.18
 */
public interface PersonalMenuOfDealer extends VertisElement {

    @Name("Имя дилера")
    @FindBy(".//div[contains(@class, 'nav__username')]")
    VertisElement userName();

    @Name("Кнопка {{ value }}")
    @FindBy(".//div[contains(@class,'HeaderMenuGroup') and contains(., '{{ value }}')]")
    VertisElement menu(@Param("value") String value);
}
