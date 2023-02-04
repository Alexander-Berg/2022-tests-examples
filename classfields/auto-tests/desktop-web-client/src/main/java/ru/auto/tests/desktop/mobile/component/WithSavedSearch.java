package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.listing.SavedSearch;

public interface WithSavedSearch {

    @Name("Блок сохранения поиска в листинге")
    @FindBy("//div[@class = 'ListingSubscription'] | " +
            "//div[contains(@class, 'CardGroupOffers__subscription')]")
    SavedSearch savedSearch();
}