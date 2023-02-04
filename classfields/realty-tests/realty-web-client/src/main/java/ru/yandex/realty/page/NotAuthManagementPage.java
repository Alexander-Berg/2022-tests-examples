package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.DomikPopup;

/**
 * Created by vicdev on 25.04.17.
 */
public interface NotAuthManagementPage extends WebPage, DomikPopup {

    @Name("Алерты валидации полей")
    @FindBy(".//div[contains(@class, 'tooltip__content')]")
    ElementsCollection<AtlasWebElement> alerts();
}
