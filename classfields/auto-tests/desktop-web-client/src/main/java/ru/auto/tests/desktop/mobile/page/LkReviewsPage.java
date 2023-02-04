package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.lk.ReviewsListItem;
import ru.auto.tests.desktop.mobile.element.lk.ReviewsStub;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface LkReviewsPage extends BasePage {

    @Name("Список отзывов пользователя")
    @FindBy("//div[contains(@class, 'MyReviewsList__item')]")
    ElementsCollection<ReviewsListItem> reviewsList();

    @Name("Кнопка «Добавить отзыв»")
    @FindBy(".//a[contains(@class, 'MyReviewsList__addReview')]")
    VertisElement addReviewButton();

    @Name("Заглушка")
    @FindBy("//div[contains(@class, 'PageMyReviews')]//div[contains(@class, 'MyReviewsStub')]")
    ReviewsStub stub();

    @Step("Получаем отзыв с индексом {i}")
    default ReviewsListItem getReview(int i) {
        return reviewsList().should(hasSize(greaterThan(i))).get(i);
    }
}
