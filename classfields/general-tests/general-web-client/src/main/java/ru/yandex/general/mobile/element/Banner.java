package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Banner extends VertisElement, Link {

    @Name("Закрыть")
    @FindBy(".//button[contains(@class, 'close')]")
    VertisElement close();

}
