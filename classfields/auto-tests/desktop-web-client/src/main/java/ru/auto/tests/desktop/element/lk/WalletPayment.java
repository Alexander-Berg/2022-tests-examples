package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WalletPayment extends VertisElement {

    @Name("Ссылка на объявление")
    @FindBy(".//a")
    VertisElement saleUrl();
}
