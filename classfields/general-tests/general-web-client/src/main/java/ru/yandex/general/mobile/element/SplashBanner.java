package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface SplashBanner extends VertisElement, Link {

    @Name("Текст")
    @FindBy(".//div[contains(@class, '_text_')]")
    VertisElement text();

}
