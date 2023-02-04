package ru.yandex.realty.element.samolet;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;

public interface SamoletOffer extends AtlasWebElement {

    @Name("Ссылка оффера")
    @FindBy(".//a[contains(@class, 'Link_js_inited')]")
    AtlasWebElement offerLink();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//button[contains(@class,'SnippetContacts__button') and not(contains(@class,'_callbackButton'))]")
    AtlasWebElement showButton();
}
