package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BodyDescription extends VertisElement {

    @Name("Ссылка на новые объявления")
    @FindBy(".//div[.= 'Новые']/following-sibling::div/a")
    VertisElement newSalesUrl();

    @Name("Ссылка на объявления с пробегом")
    @FindBy(".//div[.= 'С пробегом']/following-sibling::div/a")
    VertisElement usedSalesUrl();
}
