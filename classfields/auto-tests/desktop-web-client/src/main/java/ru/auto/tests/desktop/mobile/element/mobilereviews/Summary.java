package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Summary extends VertisElement {

    @Name("Ссылка «{{ text }}»")
    @FindBy(".//a[.= '{{ text }}'] | " +
            ".//span[.= '{{ text }}']")
    VertisElement url(@Param("text") String Text);

    @Name("Ссылка на все отзывы")
    @FindBy(".//a[contains(@class, 'ReviewSummaryRatings__reviewsCount')]")
    VertisElement allReviewsUrl();
}