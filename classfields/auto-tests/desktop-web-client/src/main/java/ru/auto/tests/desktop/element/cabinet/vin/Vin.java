package ru.auto.tests.desktop.element.cabinet.vin;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 14.11.18
 */

public interface Vin extends VertisElement, WithInput {

    @Name("Кнопка проверки VIN")
    @FindBy(".//button")
    VertisElement vinButton();

    @Name("Ошибка")
    @FindBy(".//span[contains(@class, 'TextInput__error')]")
    VertisElement errorMessage();

    @Name("Ссылка оффера")
    @FindBy(".//a[contains(@class, 'Link CheckVinTable__item-row')]")
    VertisElement nameLink();


}
