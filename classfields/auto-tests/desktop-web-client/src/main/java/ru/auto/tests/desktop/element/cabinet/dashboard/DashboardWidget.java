package ru.auto.tests.desktop.element.cabinet.dashboard;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 07.12.18
 */

public interface DashboardWidget extends VertisElement, WithButton {

    @Name("Кнопка с тремя точками")
    @FindBy(".//div[contains(@class, 'WalletBalanceWidget__menu')]")
    VertisElement balanceWidgetMenu();

}
