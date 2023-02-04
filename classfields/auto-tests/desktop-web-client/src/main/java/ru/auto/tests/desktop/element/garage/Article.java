package ru.auto.tests.desktop.element.garage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Article extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, '_title')]")
    VertisElement title();

    @Name("Характеристики")
    @FindBy(".//div[contains(@class, '_stats')]")
    VertisElement stats();

}
