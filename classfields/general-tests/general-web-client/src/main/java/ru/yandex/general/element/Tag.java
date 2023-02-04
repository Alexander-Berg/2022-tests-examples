package ru.yandex.general.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Tag extends VertisElement, Input {


    @Name("Активный")
    @FindBy(".//div[contains(@class, '_active')]")
    VertisElement active();

}
