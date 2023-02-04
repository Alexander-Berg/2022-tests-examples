package ru.auto.tests.desktop.element.cabinet.calls;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 24.05.18
 */
public interface Call extends VertisElement {

    @Name("Цена")
    @FindBy(".//div[@class = 'CallsListingItem__price']")
    VertisElement price();

    @Name("Кнопка «...» для вызова меню")
    @FindBy(".//*[contains(@class, 'menuIcon')]")
    VertisElement menuButton();

}
