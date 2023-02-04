package ru.auto.tests.desktop.element.mag;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface MagArticle extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//span[contains(@class, 'Journal__item-title')]")
    VertisElement title();

}
