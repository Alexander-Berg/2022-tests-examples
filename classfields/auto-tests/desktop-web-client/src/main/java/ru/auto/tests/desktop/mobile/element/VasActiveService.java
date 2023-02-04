package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface VasActiveService extends VertisElement {

    @Name("Неактивный тумблер автопродления")
    @FindBy(".//label[contains(@class, 'Toggle') and not(contains(@class, 'Toggle_checked'))]")
    VertisElement autoprolongTogglerInactive();

    @Name("Aктивный тумблер автопродления")
    @FindBy(".//label[contains(@class, 'Toggle_checked')]")
    VertisElement autoprolongTogglerActive();
}
