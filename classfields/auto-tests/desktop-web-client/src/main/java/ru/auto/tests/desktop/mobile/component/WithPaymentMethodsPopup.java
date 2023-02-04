package ru.auto.tests.desktop.mobile.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.element.PaymentMethodsFrameContent;

public interface WithPaymentMethodsPopup {

    @Name("Фрейм поп-апа способов оплаты")
    @FindBy("//iframe[contains(@class, 'PaymentDialogContainer__frame')]")
    VertisElement paymentMethodsFrame();

    @Name("Содержимое фрейма поп-апа способов оплаты")
    @FindBy("//div[contains(@class, 'Billing_platform_mobile')] | " +
            "//body | " +
            "//div[contains(@class, 'BillingMobile')]")
    PaymentMethodsFrameContent paymentMethodsFrameContent();

    @Name("Кнопка «Закрыть»")
    @FindBy(".//div[@class = 'office-service__hide-payments']")
    VertisElement closeButton();
}