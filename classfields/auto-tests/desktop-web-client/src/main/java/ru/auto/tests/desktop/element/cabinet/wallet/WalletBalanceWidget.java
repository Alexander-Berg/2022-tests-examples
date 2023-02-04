package ru.auto.tests.desktop.element.cabinet.wallet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.cabinet.PopupBillingBlock;

public interface WalletBalanceWidget extends VertisElement {

    @Name("Кнопка «Пополнить счёт»")
    @FindBy(".//button[contains(@class, 'BalanceRecharge__paymentButton')]")
    VertisElement rechargeButton();

    @Name("Поп-ап «Пополнить кошелёк»")
    @FindBy("//div[contains(@class, 'BalanceRecharge__modal') and contains(@class, 'Modal_visible')]")
    PopupBillingBlock popupBillingBlock();
}
