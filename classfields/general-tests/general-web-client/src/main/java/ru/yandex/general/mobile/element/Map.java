package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Map extends Input, Button {

    @Name("Пин на карте")
    @FindBy(".//div[contains(@class, '_placemarkContent')]")
    VertisElement pin();

}
