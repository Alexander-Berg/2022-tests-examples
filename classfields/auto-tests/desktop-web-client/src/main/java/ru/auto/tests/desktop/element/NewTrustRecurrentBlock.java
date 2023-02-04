package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface NewTrustRecurrentBlock extends VertisElement {

    @Name("Свитчер автопродления активированный")
    @FindBy(".//label[contains(@class, 'Toggle_checked')]")
    VertisElement enabledSwitcher();

    @Name("Свитчер автопродления неактивированный")
    @FindBy(".//label[not(contains(@class, 'Toggle_checked'))]")
    VertisElement disabledSwitcher();

}
