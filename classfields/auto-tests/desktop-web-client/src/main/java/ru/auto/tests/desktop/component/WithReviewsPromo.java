package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.ReviewsPromo;

public interface WithReviewsPromo {

    @Name("Промо отзывов после снятия с продажи")
    @FindBy("//div[@class = 'SalesReviewsPromoDialog']")
    ReviewsPromo reviewsPromo();
}
