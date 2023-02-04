package ru.auto.tests.desktop.mobile.element;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface ReviewsPlusMinusPopup extends VertisElement {

    @Name("Список плюсов")
    @FindBy(".//div[@class = 'ReviewsFeaturesModal__section'][1]" +
            "//div[contains(@class, 'ReviewsFeaturesSnippet__featureLabel')]")
    ElementsCollection<VertisElement> plusList();

    @Name("Список отзывов в плюсе")
    @FindBy(".//div[@class = 'ReviewsFeaturesModal__section'][1]//a")
    ElementsCollection<VertisElement> plusReviewsList();

    @Name("Кнопка «Закрыть»")
    @FindBy("../..//div[contains(@class, 'Modal__closer')]")
    VertisElement closeButton();

    @Step("Получаем плюс с индексом {i}")
    default VertisElement getPlus(int i) {
        return plusList().should(hasSize(greaterThan(i))).get(i);
    }

    @Step("Получаем отзыв с индексом {i}")
    default VertisElement getPlusReview(int i) {
        return plusReviewsList().should(hasSize(greaterThan(i))).get(i);
    }
}