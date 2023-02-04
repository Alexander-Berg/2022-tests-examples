package ru.auto.tests.desktop.mobile.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithSelect;
import ru.auto.tests.desktop.mobile.element.YaKassaFrame;

/**
 * @author Anton Tsyganov (jenkl)
 */
public interface PaymentPage extends BasePage, WithSelect {

    @Name("Форма для ввода данных карты")
    @FindBy("//form")
    YaKassaFrame yaKassaFrame();

    @Name("Сообщение об успешной оплате")
    @FindBy("//div[contains(@class, 'BillingPaymentStatus_platform_mobile')]")
    VertisElement successMessage();
}
