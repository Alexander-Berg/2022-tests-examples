package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.mobile.element.BillingPopup;

public interface WithBillingPopup {

    @Name("Поп-ап оплаты")
    @FindBy("//div[contains(@class, 'ModalDialogPayment')]//div[@class = 'Modal__content']")
    BillingPopup billingPopup();
}