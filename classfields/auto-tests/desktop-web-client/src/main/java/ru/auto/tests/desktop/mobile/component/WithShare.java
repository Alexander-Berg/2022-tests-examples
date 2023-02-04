package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithShare {

    @Name("Иконка Вконтакте")
    @FindBy(".//a[contains(@href, 'vk.com')]")
    VertisElement vkIcon();

}
