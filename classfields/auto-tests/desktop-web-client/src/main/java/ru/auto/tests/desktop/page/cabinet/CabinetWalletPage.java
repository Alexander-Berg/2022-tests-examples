package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.cabinet.wallet.Graph;
import ru.auto.tests.desktop.element.cabinet.wallet.WalletBalanceWidget;
import ru.auto.tests.desktop.element.cabinet.wallet.WalletHeader;
import ru.auto.tests.desktop.element.cabinet.wallet.WalletHistory;
import ru.auto.tests.desktop.element.cabinet.wallet.WalletTotal;

public interface CabinetWalletPage extends BasePage {

    @Name("Шапка кошелька")
    @FindBy("//div[contains(@class, 'WalletHeader__container')]")
    WalletHeader walletHeader();

    @Name("Блок «Расходы на размещение и дополнительные услуги»")
    @FindBy("//div[contains(@class, 'WalletDashboard__walletGraphWrapper')]")
    Graph graph();

    @Name("Балансовый виджет")
    @FindBy("//div[contains(@class, 'WalletDashboard__balanceWidgetWrapper')]")
    WalletBalanceWidget walletBalanceWidget();

    @Name("Блок «Итого за период»")
    @FindBy("//div[contains(@class, 'WalletTotal__wrapper')]")
    WalletTotal walletTotal();

    @Name("Подробная история")
    @FindBy("//div[contains(@class, 'WalletHistory__container')]")
    WalletHistory walletHistory();

    @Name("Иконка модерации")
    @FindBy("//*[contains(@class, 'IconSvg_timer')]")
    VertisElement moderationIcon();
}
