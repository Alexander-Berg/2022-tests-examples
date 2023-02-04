package ru.auto.tests.desktop.mobile.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Gallery extends VertisElement {

    @Name("Фото")
    @FindBy(".//img")
    VertisElement img();

}
