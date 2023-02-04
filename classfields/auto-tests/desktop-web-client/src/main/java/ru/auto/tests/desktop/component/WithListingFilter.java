package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.listing.Filter;

public interface WithListingFilter {

    @Name("Фильтр поиска")
    @FindBy("//*[contains(@class, 'search-form ')] | " +
            "//div[contains(@class, 'ListingCarsFilters')] | " +
            "//div[contains(@class, 'ListingMotoFilters')] | " +
            "//div[contains(@class, 'ListingTrucksFilters')] | " +
            "//div[contains(@class, 'PageSalon__filters')] | " +
            "//div[contains(@class, 'DealerDistributorFilters')]")
    Filter filter();
}