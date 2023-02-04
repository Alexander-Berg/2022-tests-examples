package ru.auto.tests.desktop.element.forms;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;

public interface PlusMinusPopup extends VertisElement, WithCheckbox {

    @Name("Плюс или минус «{{ index }}»")
    @FindBy(".//div[@class = 'ProsAndConsItem__list-item ' and .= '{{ index }}']")
    VertisElement plusMinus(@Param("index") String index);
}