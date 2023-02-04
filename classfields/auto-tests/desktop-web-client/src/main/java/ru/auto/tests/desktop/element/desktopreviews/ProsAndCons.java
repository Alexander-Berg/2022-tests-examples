package ru.auto.tests.desktop.element.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithReviewsPlusMinusPopup;

public interface ProsAndCons extends VertisElement, WithReviewsPlusMinusPopup {

    @Name("Оценка автора отзыва")
    @FindBy(".//div[contains(@class, 'ReviewProsAndCons__summary__title')]/following-sibling::div")
    VertisElement authorRating();

    @Name("Ссылка на все отзывы")
    @FindBy(".//a[contains(@class, 'Review__ratingLink')]")
    VertisElement allReviewsUrl();

    @Name("Кнопка «Все плюсы и минусы»")
    @FindBy(".//div[contains(@class, 'ReviewProsAndCons__summary__full')]")
    VertisElement plusAndMinusButton();
}
