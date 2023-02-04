package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ReviewsPromo extends VertisElement {

    @Name("Заголовок")
    @FindBy(".//h1")
    VertisElement title();

    @Name("Кнопка «Оставить отзыв»")
    @FindBy(".//div[@class = 'SalesReviewsPromoDialogAction']/a")
    VertisElement saveButton();

    @Name("Кнопка «Не оставлять»")
    @FindBy(".//div[@class = 'SalesReviewsPromoDialogAction']/button[contains(@class, 'Button_color_white')]")
    VertisElement cancelButton();
}