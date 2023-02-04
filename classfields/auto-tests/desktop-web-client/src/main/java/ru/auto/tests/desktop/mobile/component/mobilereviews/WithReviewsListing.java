package ru.auto.tests.desktop.mobile.component.mobilereviews;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.mobilereviews.ListingReview;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface WithReviewsListing {

    @Name("Список отзывов")
    @FindBy(".//div[contains(@class, 'ReviewsList__item')] | " +
            ".//div[contains(@class, 'ReviewSnippet_special')]")
    ElementsCollection<ListingReview> reviewsList();

    @Step("Получаем отзыв с индексом {i}")
    default ListingReview getReview(int i) {
        return reviewsList().should(hasSize(greaterThan(i))).get(i);
    }
}
