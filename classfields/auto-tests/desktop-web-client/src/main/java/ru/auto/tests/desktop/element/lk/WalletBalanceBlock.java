package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface WalletBalanceBlock extends VertisElement, WithButton {

    String DEPOSIT = "Пополнить";

    @Name("Баланс кошелька")
    @FindBy(".//span[contains(@class, 'MyWalletBalance__balance')]")
    VertisElement balance();

}
