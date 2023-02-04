package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Ratings extends VertisElement {

    @Name("Ссылка на все отзывы")
    @FindBy(".//a[contains(@class, '__reviewsCount')]")
    VertisElement allReviewsUrl();

    @Name("Ссылка «{{ text }}»")
    @FindBy(".//a[contains(., '{{ text }}')] | " +
            ".//span[contains(., '{{ text }}')]")
    VertisElement url(@Param("text") String Text);
}
