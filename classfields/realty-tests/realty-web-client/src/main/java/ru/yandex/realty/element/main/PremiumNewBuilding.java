package ru.yandex.realty.element.main;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Link;

public interface PremiumNewBuilding extends Link {

    @Name("Тайтл")
    @FindBy(".//h3[@class='MinifiedSerpItem__title']")
    AtlasWebElement title();
}
