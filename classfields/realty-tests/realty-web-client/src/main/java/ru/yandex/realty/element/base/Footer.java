package ru.yandex.realty.element.base;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;

public interface Footer extends Link {

    @Name("Контент")
    @FindBy(".//div[@class='ContentWidth MegaFooter__content']")
    AtlasWebElement content();

    @Name("Ссылка «{{ value }}»")
    @FindBy(".//a[contains(@class, 'SocialNetworks__link')][.//*[contains(@class,'IconSvg_{{ value }}')]]")
    AtlasWebElement socialNetLink(@Param("value") String value);
}
