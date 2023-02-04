package ru.yandex.realty.mobile.element.newbuilding;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;

public interface FeaturesBlock extends Button {

    @Name("Список особенностей")
    @FindBy(".//div[contains(@class,'CardFeaturesItem__container')]")
    ElementsCollection<AtlasWebElement> featuresList();
}
