package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface InputClear extends VertisElement {

    @Name("Очистить инпут")
    @FindBy("../span")
    VertisElement clearInput();

    @Name("Очистить инпут")
    @FindBy("..//button[contains(@class, 'DeleteIcon')]")
    VertisElement clearButton();

}
