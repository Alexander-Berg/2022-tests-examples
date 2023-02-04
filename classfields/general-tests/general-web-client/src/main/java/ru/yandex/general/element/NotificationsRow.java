package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface NotificationsRow extends VertisElement, Checkbox {

    @Name("Ячейка «{{ value }}»")
    @FindBy(".//div[contains(@class, '_cellCheckbox')][{{ value }}]/span")
    Checkbox cell(@Param("value") int value);

}
