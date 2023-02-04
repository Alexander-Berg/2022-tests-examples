package ru.yandex.realty.mobile.element;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface Footer extends AtlasWebElement, Link {

    String DZEN = "dzen";
    String TWITTER = "twitter";
    String VK = "vk";

    @Name("Ссылка на соц. сеть «{{ value }}»")
    @FindBy(".//a[contains(@class, 'SocialNetworks__link')][.//*[contains(@class,'IconSvg_{{ value }}')]]")
    AtlasWebElement socialLink(@Param("value") String value);

    @Name("Баннер приложения")
    @FindBy(".//div[contains(@class, 'MobileAppAd')]")
    Link appAd();

}
