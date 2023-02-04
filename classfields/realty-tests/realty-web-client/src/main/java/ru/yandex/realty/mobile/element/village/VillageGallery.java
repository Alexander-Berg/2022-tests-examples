package ru.yandex.realty.mobile.element.village;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.mobile.element.CloseCross;
import ru.yandex.realty.mobile.element.Link;

public interface VillageGallery extends CloseCross, Link, Button {

    @Name("Кнопка селектора")
    @FindBy(".//span[contains(@class,'VillageGallerySnippet__hint')]")
    AtlasWebElement promoHint();
}
