package ru.auto.tests.desktop.mobile.component.mobilereviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.mobilereviews.Presets;

public interface WithPresets {

    @Name("Пресеты")
    @FindBy(".//div[contains(@class, 'PageReviewsIndex__reviewSliders')]")
    Presets presets();
}