package ru.auto.tests.desktop.element.ads;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface AdBanner extends VertisElement {

    @Name("Кликабельный элемент на рекламном банере")
    @FindBy(".//*[contains(@href, 'an.yandex.ru/count') or contains(@id, 'ya_partner') or " +
            "contains(@href, 'awaps.yandex.net') or contains(@href, 'ads.adfox.ru/')] | " +
            ".//ya-recommendation-widget")
    ElementsCollection<VertisElement> bannerLinks();
}
