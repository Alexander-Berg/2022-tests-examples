package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BreadcrumbsItem extends VertisElement {

    @Name("Ссылка")
    @FindBy(".//a")
    VertisElement url();
}