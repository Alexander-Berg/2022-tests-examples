package ru.auto.tests.desktop.component.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.element.desktopreviews.Presets;

public interface WithPresets {

    @Name("Пресеты")
    @FindBy(".//div[contains(@class, 'PageReviewsIndex__tabs')]")
    Presets presets();
}