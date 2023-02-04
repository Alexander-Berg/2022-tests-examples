package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.BillingPopup;

public interface WithBillingModalPopup {

    @Name("Фрейм поп-апа оплаты")
    @FindBy("//div[@class = 'PaymentDialogContainer__frame']/iframe | " +
            "//div[@class = 'pay-card__iframe']/iframe | " +
            "//iframe[@class = 'PaymentDialogContainer__frame'] | " +
            "//iframe[@class = 'billing-container__frame']")
    VertisElement billingPopupFrame();

    @Name("Поп-ап оплаты")
    @FindBy("//div[contains(@class, ' billing_js_inited')] | " +
            "//div[contains(@class, 'Layout_page_billing')] | " +
            "//div[contains(@class, 'Billing')]")
    BillingPopup billingPopup();

    @Name("Иконка закрытия поп-апа оплаты")
    @FindBy("//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__closer')] | " +
            "//div[contains(@class, 'ModalDialogCloser')] | " +
            "//div[contains(@class, 'popup__closer_color_white')]")
    VertisElement billingPopupCloseButton();

}
