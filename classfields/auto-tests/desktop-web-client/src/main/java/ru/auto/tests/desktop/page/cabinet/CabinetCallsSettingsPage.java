package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface CabinetCallsSettingsPage extends CabinetCallsPage {

    @Name("Активный тумблер в блоке «{{ text }}»")
    @FindBy("//div[contains(@class, 'CallTrackingSettingsItem') and contains(., '{{ text }}')]" +
            "//label[contains(@class, 'Toggle_checked')]")
    VertisElement activeToggle(@Param("text") String text);

    @Name("Неактивный тумблер в блоке «{{ text }}»")
    @FindBy("//div[contains(@class, 'CallTrackingSettingsItem') and contains(., '{{ text }}')]" +
            "//label[not(contains(@class, 'Toggle_checked'))]")
    VertisElement inactiveToggle(@Param("text") String text);
}