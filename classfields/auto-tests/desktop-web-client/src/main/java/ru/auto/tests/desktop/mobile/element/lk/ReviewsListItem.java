package ru.auto.tests.desktop.mobile.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface ReviewsListItem extends VertisElement, WithButton {

    @Name("Заголовок")
    @FindBy(".//a[contains(@class, 'MyReview__title')]")
    VertisElement title();

}
