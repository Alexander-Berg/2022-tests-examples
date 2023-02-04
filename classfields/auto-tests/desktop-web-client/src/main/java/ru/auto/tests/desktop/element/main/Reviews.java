package ru.auto.tests.desktop.element.main;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Reviews extends VertisElement {

    @Name("Список отзывов")
    @FindBy(".//div[contains(@class, 'IndexReviews__review Index__col')]")
    ElementsCollection<ReviewsItem> reviewsList();

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'Index__title-link')]")
    VertisElement title();

    @Name("Кнопка «Добавить отзыв»")
    @FindBy(".//div[@class = 'IndexReviews__add-review-button']/a")
    VertisElement addReviewButton();

    @Name("Ссылка «Все отзывы»")
    @FindBy(".//a[contains(@class, 'Index__all-link')]")
    VertisElement allReviewsUrl();

    @Step("Получаем отзыв с индексом {i}")
    default ReviewsItem getReview(int i) {
        return reviewsList().should(hasSize(greaterThan(i))).get(i);
    }
}