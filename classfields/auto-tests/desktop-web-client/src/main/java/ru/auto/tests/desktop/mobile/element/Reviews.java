package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface Reviews extends VertisElement, WithButton {

    @Name("Список отзывов")
    @FindBy(".//div[@class = 'IndexBlock__item'] |" +
            ".//div[@class = 'ReviewsRelatedCardList__item'] |" +
            ".//div[@class = 'ReviewsRelatedCardList'] |" +
            ".//div[@class = 'OfferAmpReviewsRelated__item'] ")
    ElementsCollection<VertisElement> reviewsList();

    @Step("Получаем отзыв с индексом {i}")
    default VertisElement getReview(int i) {
        return reviewsList().should(hasSize(greaterThan(i))).get(i);
    }
}
