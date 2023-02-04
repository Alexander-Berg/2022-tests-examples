package ru.auto.tests.desktop.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithReviewsPlusMinusPopup;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Reviews extends VertisElement, WithReviewsPlusMinusPopup, WithButton {

    @Name("Заголовок")
    @FindBy(".//div[@class = 'CardReviews__header'] |" +
            "//h2[contains(@class, 'RatingSnippetList__title')]")
    VertisElement header();

    @Name("Список отзывов")
    @FindBy(".//li[contains(@class, 'carousel__item')] | " +
            ".//div[contains(@class, '__RatingSnippetList_item')] | " +
            ".//li[contains(@class, 'CarouselUniversal__item')]")
    ElementsCollection<VertisElement> reviewsList();

    @Name("Список плюсов/минусов во вкладке")
    @FindBy(".//div[contains(@class, 'CardReviews__item')]")
    ElementsCollection<VertisElement> tabPlusMinusList();

    @Step("Получаем плюс/минус с индексом {i}")
    default VertisElement getTabPlusMinus(int i) {
        return tabPlusMinusList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем отзыв с индексом {i}")
    default VertisElement getReview(int i) {
        return reviewsList().should(hasSize(greaterThan(i))).get(i);
    }
}
