package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Preset extends VertisElement {

    @Name("Кнопка «Все»")
    @FindBy(".//div[contains(@class, 'PageReviewsIndex__sliderListingAllLink')]/a")
    VertisElement showAllButton();
}
