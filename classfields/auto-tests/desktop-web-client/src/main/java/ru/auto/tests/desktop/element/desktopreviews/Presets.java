package ru.auto.tests.desktop.element.desktopreviews;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Presets extends VertisElement {

    @Name("Пресет «{{ text }}»")
    @FindBy(".//div[contains(@class, 'PageReviewsIndex__tab') and . = '{{ text }}']")
    VertisElement preset(@Param("text") String text);

    @Name("Активный пресет")
    @FindBy(".//div[contains(@class, 'PageReviewsIndex__tab_active')]")
    VertisElement activePreset();
}
