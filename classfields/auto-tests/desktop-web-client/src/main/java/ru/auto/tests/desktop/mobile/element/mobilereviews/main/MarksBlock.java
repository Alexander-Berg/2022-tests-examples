package ru.auto.tests.desktop.mobile.element.mobilereviews.main;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface MarksBlock extends VertisElement {

    @Name("Ссылка «Все»")
    @FindBy(".//div[contains(@class, 'PageReviewsIndex__markLogosTitle')]/a")
    VertisElement allReviewsUrl();

    @Name("Марка «{{ text }}»")
    @FindBy(".//a[contains(@class, 'MarkLogosListItem') and .= '{{ text }}']")
    VertisElement mark(@Param("text") String Text);
}
