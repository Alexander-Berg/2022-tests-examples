package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.cabinet.PopupBillingBlock;
import ru.auto.tests.desktop.element.cabinet.dashboard.DashboardWidget;
import ru.auto.tests.desktop.element.cabinet.dashboard.PromocodePopup;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 07.12.18
 */
public interface CabinetDashboardPage extends CabinetOffersPage {

    @Name("Дашборд")
    @FindBy("//div[@class = 'Dashboard']")
    VertisElement dashboard();

    @Name("Поп-ап пополнения счета")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    PopupBillingBlock popupBillingBlock();

    @Name("Поп-ап «Использование промокода»")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    PromocodePopup promocodePopup();

    @Name("Селект выбора плательщика")
    @FindBy(".//div[contains(@class, 'MenuItem') and contains(., '{{ value }}')]")
    VertisElement selectPayer(@Param("value") String value);

    @Name("Виджет «{{ text }}»")
    @FindBy(".//div[contains(@class, 'DashboardWidget') and contains(., '{{ text }}')]")
    DashboardWidget dashboardWidget(@Param("text") String Text);

    @Name("Пункт «{{ text }}» в выпадающем меню")
    @FindBy(".//div[contains(@class, 'WalletBalanceWidget__menuItem') and .= '{{ text }}']")
    VertisElement balanceWidgetButton(@Param("text") String Text);
}
