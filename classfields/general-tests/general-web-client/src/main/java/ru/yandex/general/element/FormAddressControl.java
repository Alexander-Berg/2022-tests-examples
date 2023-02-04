package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FormAddressControl extends VertisElement, Input, Link {

    String DELETE = "Удалить";

    @Name("Ошибка под инпутом адреса")
    @FindBy(".//span[@class = 'Textinput-Hint']")
    VertisElement inputHint();

}
