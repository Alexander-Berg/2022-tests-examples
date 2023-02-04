package ru.auto.tests.desktop.element.cabinet.settings;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 28.08.18
 */
public interface Section extends VertisElement {

    @Name("Активный тумблер в блоке «{{ text }}»")
    @FindBy(".//div[contains(@class, '__subHeading')][contains(., '{{ text }}')]" +
            "//label[contains(@class, 'Toggle_checked')] | " +
            ".//div[contains(@class, '__header')][contains(., '{{ text }}')]" +
            "//label[contains(@class, 'Toggle_checked')]")
    VertisElement activeToggle(@Param("text") String text);

    @Name("Неактивный тумблер в блоке «{{ text }}»")
    @FindBy(".//div[contains(@class, '__subHeading')][contains(., '{{ text }}')]" +
            "//label[not(contains(@class, 'Toggle_checked'))] | " +
            ".//div[contains(@class, '__header')][contains(., '{{ text }}')]" +
            "//label[not(contains(@class, 'Toggle_checked'))]")
    VertisElement inactiveToggle(@Param("text") String text);
}
