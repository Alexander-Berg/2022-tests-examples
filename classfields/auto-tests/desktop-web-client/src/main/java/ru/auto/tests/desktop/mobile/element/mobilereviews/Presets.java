package ru.auto.tests.desktop.mobile.element.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Presets extends VertisElement {

    @Name("Пресет «{{ text }}»")
    @FindBy(".//div[contains(@class, 'PageReviewsIndex__sliderListing') and .//div[.= '{{ text }}']]")
    Preset preset(@Param("text") String text);
}