package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BodyItem extends VertisElement {

    @Name("Ссылка")
    @FindBy(".//a")
    VertisElement url();

    @Name("Фото")
    @FindBy(".//img")
    VertisElement image();

    @Name("Ссылка на новые объявлений")
    @FindBy(".//a[contains(text(), 'нов')]")
    VertisElement newSalesUrl();

    @Name("Ссылка на объявления с пробегом")
    @FindBy(".//a[contains(text(), 'пробег')]")
    VertisElement usedSalesUrl();
}
