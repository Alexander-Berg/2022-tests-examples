package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.SavedSearchesPopup;

public interface WithSavedSearchesPopup {

    @Name("Поп-ап сохранённого поиска")
    @FindBy("//div[contains(@class, 'ListingFilterSubscriptionDumb__popup')] | " +
            "//div[contains(@class, 'SubscriptionItemDesktop')]")
    SavedSearchesPopup savedSearchesPopup();
}