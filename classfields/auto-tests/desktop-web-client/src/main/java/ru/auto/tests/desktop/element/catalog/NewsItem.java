package ru.auto.tests.desktop.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface NewsItem extends VertisElement {

    @Name("Название модели")
    @FindBy(".//div[contains(@class, 'tile__header')]")
    VertisElement title();

}
