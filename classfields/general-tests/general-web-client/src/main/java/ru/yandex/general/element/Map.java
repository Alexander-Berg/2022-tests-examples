package ru.yandex.general.element;

import io.qameta.allure.junit4.DisplayName;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Map extends VertisElement {

    @Name("Пины на карте")
    @FindBy(".//div[contains(@class, '_placemarkContent')]")
    ElementsCollection<VertisElement> pinList();

}
