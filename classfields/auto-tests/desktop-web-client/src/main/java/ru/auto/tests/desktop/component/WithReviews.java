package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.Reviews;

public interface WithReviews {

    @Name("Блок отзывов")
    @FindBy("//div[contains(@class, 'reviews-summary')] | " +
            "//div[contains(@class, 'CardReviews')] | " +
            "//div[contains(@class, 'CardGroupAboutModelReviews')] | " +
            "//div[@class = 'VinReportReviews']|" +
            "//div[contains(@class, 'RatingSnippetList__container')]")
    Reviews reviews();
}