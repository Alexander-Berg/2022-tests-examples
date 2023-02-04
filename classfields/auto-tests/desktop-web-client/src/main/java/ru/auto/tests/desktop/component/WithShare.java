package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.Share;

public interface WithShare {

    @Name("Кнопка «Поделиться в соцсетях»")
    @FindBy(".//span[contains(@class, 'icon_type_share')] | " +
            ".//div[contains(@class, 'ButtonShare-module__container')] | " +
            ".//div[@class = 'ShareControl'] | " +
            ".//div[contains(@class, 'ButtonShare_size_')]")
    VertisElement shareButton();

    @Name("Выпадушка поделяшек")
    @FindBy("//*[contains(@class, 'yashare__popup')] | " +
            "//div[contains(@class, 'Share__popup') and contains(@class, 'Popup_visible')] | " +
            "//div[contains(@class, 'ya-share2 ya-share2_inited')]")
    Share shareDropdown();

    @Name("Кнопка «Поделиться в VK»")
    @FindBy(".//li[contains(@class, '_service_vk')]/a")
    VertisElement vkButton();
}
