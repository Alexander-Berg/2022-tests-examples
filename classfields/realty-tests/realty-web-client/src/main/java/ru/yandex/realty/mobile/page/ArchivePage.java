package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;

public interface ArchivePage extends BasePage, Button {

    @Name("Начните вводитьт адрес")
    @FindBy("//input[@name='address']")
    AtlasWebElement address();

    @Name("Саджест вариантов адреса")
    @FindBy("//ul/li[contains(@class,'_suggest-list-item')]")
    ElementsCollection<AtlasWebElement> suggestListItems();

    @Name("Архивные офферы")
    @FindBy("//div[@class='OffersArchiveCard']")
    ElementsCollection<AtlasWebElement> archiveOffers();

    @Name("Выпадушка комнатность")
    @FindBy("//div[@class='OffersArchive__search-filters']//div[@class='Collapse']")
    AtlasWebElement roomsCollapser();

    @Name("Селектор количества комнат")
    @FindBy("//div[contains(@class,'SelectFormControl_name_roomsTotal')]//select")
    AtlasWebElement roomsSelector();

    @Name("Опция количества комнат {{ value }}")
    @FindBy("//div[@class='OffersArchive__search-filters']//select/option[text()='{{ value }}']")
    AtlasWebElement option(@Param("value") String value);
}
