package ru.auto.tests.desktop.mobile.element.garage;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface CardReviews extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'CardReviews__header')]")
    VertisElement title();

    @Name("Список отзывов")
    @FindBy(".//div[@class = 'RatingSnippet RatingSnippet_full']")
    ElementsCollection<VertisElement> reviewsList();

    @Step("Получаем отзыв с индексом {i}")
    default VertisElement getReview(int i) {
        return reviewsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Список плюсов/минусов во вкладке")
    @FindBy(".//div[@class = 'CardReviews__item']")
    ElementsCollection<VertisElement> plusMinusList();

    @Step("Получаем плюс/минус с индексом {i}")
    default VertisElement getPlusMinus(int i) {
        return plusMinusList().should(hasSize(greaterThan(i))).get(i);
    }
}
