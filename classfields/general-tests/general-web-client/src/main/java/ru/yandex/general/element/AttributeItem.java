package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AttributeItem extends VertisElement {

    @Name("Имя")
    @FindBy("./span[contains(@class, '_name')]")
    VertisElement name();

    @Name("Значение")
    @FindBy("./span[contains(@class, '_value')]")
    VertisElement value();

}
