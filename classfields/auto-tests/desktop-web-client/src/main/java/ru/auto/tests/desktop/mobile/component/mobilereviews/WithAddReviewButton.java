package ru.auto.tests.desktop.mobile.component.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithAddReviewButton extends VertisElement {

    @Name("Кнопка «Добавить отзыв»")
    @FindBy(".//a[.= 'Добавить отзыв']")
    VertisElement addReviewButton();
}
