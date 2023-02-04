package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Image extends VertisElement {

    String SRC = "src";

    @Name("Изображение")
    @FindBy(".//img")
    VertisElement image();

}
