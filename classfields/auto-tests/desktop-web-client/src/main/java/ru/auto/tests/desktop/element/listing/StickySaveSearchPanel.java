package ru.auto.tests.desktop.element.listing;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface StickySaveSearchPanel extends VertisElement {

    String SAVE_SEARCH = "Сохранить поиск";
    String SAVED = "Сохранён";

    @Name("Кнопка сохранения поиска")
    @FindBy(".//div[contains(@class, 'ListingSearchSave__control')]")
    VertisElement saveButton();

}
