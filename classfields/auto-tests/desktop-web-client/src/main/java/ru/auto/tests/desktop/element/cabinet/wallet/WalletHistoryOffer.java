package ru.auto.tests.desktop.element.cabinet.wallet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WalletHistoryOffer extends VertisElement {

    @Name("Название (марка, модель)")
    @FindBy(".//a[contains(@class, 'WalletHistoryOffer__name')]")
    VertisElement title();

    @Name("Ссылка «Подробная статистика»")
    @FindBy(".//a[contains(@class, 'WalletHistoryOffer__statsLink')]")
    VertisElement statsUrl();
}