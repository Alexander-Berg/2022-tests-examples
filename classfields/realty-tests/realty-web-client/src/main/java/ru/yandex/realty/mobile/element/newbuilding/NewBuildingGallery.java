package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.CloseCross;
import ru.yandex.realty.mobile.element.Link;

public interface NewBuildingGallery extends CloseCross, Link, Button {

    @Name("Иконка рекламы")
    @FindBy(".//span[contains(@class,'NewbuildingGallerySnippet__hint')]")
    AtlasWebElement promoHint();
}
