package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FormAddressControl extends VertisElement, Input {

    @Name("Ошибка под инпутом адреса")
    @FindBy(".//span[@class = 'Textinput-Hint']")
    VertisElement inputHint();

    @Name("Иконка удаления адреса")
    @FindBy(".//span[contains(@class, '_trashIcon')]")
    VertisElement delete();

}
