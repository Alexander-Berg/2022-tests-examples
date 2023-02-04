package ru.yandex.realty.element.offercard;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;

public interface OfferCardSummary extends PhoneBlock {

    @Name("Тэг «{{ value }}»")
    @FindBy(".//div[contains(@class,'OfferCardSummaryTags__tag') and contains(.,'{{ value }}')]")
    AtlasWebElement tag(@Param("value") String value);

    @Name("Добавить к сравнению")
    @FindBy(".//button[.//*[contains(@class,'IconSvg_favorite')]]")
    AtlasWebElement addToFavButton();

    @Name("Доп действия")
    @FindBy(".//button[.//*[contains(@class,'IconSvg_more')]]")
    AtlasWebElement moreButton();
}
