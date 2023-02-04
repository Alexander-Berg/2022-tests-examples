package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface FreePromoBanner extends AtlasWebElement {

    @Name("Закрыть баннер")
    @FindBy("//div[contains(@class, 'free-placement-promo__close')]")
    AtlasWebElement close();
}
