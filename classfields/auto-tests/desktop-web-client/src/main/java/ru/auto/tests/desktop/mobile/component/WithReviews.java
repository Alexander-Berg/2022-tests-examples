package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.Reviews;
import ru.auto.tests.desktop.mobile.element.ReviewsPlusMinus;

public interface WithReviews {

    @Name("Блок отзывов")
    @FindBy("//div[contains(@class, 'CardReviews-module__CardReviews')] | " +
            "//div[@name = 'reviews'] | " +
            "//section[contains(@class, 'CardReviewsRelated')] | " +
            "//div[contains(@class, 'IndexBlock IndexReviews')] | " +
            "//div[contains(@class, 'VinReportReviews')] | " +
            "//div[contains(@class, 'CardReviewsMobile')] |" +
            "//div[contains(@class, 'OfferAmpReviewsRelated')] ")
    Reviews reviews();

    @Name("Блок плюсов/минусов")
    @FindBy("//section[contains(@class, 'CardReviewsFeatures')] |" +
            "//section[contains(@class, 'OfferAmpReviewsFeatures')]")
    ReviewsPlusMinus reviewsPlusMinus();
}