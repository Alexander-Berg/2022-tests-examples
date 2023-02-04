package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface UserBlock extends VertisElement, Image {

    @Name("Имя")
    @FindBy(".//span[contains(@class, '_textMedium')]")
    VertisElement name();

    @Name("Email")
    @FindBy(".//span[contains(@class, '_subtext')][1]")
    VertisElement email();

    @Name("ID")
    @FindBy(".//span[contains(@class, '_subtext')][2]")
    VertisElement id();

}
