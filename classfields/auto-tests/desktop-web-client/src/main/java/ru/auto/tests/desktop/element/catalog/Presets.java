package ru.auto.tests.desktop.element.catalog;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Presets extends VertisElement {

    @Name("Пресет '{{ text }}'")
    @FindBy(".//div[contains(@class, 'catalog-presets__menu-inner')]//a[text() = '{{ text }}']")
    VertisElement title(@Param("text") String text);

    @Name("Активный пресет")
    @FindBy(".//div[contains(@class, 'tabs__item_state_active')]")
    VertisElement activePreset();

    @Name("Содержимое пресета")
    @FindBy(".//div[contains(@class, 'catalog-presets__content') and not(contains(@class, 'hidden'))]")
    PresetContent content();
}
