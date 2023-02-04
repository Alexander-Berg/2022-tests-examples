package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface NotificationSettings extends VertisElement, Checkbox {

    String PUSH_ROW = "Пуш на телефон";
    String EMAIL_ROW = "Эл. почта";
    String SMS_ROW = "Смс";

    @Name("Строка «{{ value }}»")
    @FindBy(".//div[contains(@class, '_settingsRow')][./span[contains(., '{{ value }}')]]")
    Checkbox row(@Param("value") String value);

}
