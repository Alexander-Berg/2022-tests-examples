package ru.auto.tests.desktop.page.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithContactsPopup;
import ru.auto.tests.desktop.component.desktopreviews.WithFilters;
import ru.auto.tests.desktop.component.desktopreviews.WithPresets;
import ru.auto.tests.desktop.component.desktopreviews.WithReviewsListing;
import ru.auto.tests.desktop.page.BasePage;

public interface ReviewsMainPage extends BasePage, WithFilters, WithPresets, WithReviewsListing, WithContactsPopup {

    int REVIEWS_LISTING_SIZE = 20;

    @Name("Топовый отзыв - «{{ text }}»")
    @FindBy("//div[contains(@class, 'PageReviewsIndex__specialGridCell') and .//span[.= '{{ text }}']]")
    VertisElement topReview(@Param("text") String text);
}
