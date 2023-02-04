package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface PaymentMethodsFrameContent extends VertisElement, WithSelect, WithCheckbox, WithButton {

    @Name("Заголовок»")
    @FindBy(".//div[contains(@class, 'BillingHeader__title')]")
    VertisElement title();

    @Name("Подзаголовок")
    @FindBy(".//div[contains(@class, 'BillingHeader__subtitle')]")
    VertisElement subTitle();

    @Name("Пакет отчётов «{{ text }}»")
    @FindBy(".//div[contains(@class, 'BillingVinReportSelector__radio') and contains(., '{{ text }}')]")
    VertisElement vinPackageName(@Param("text") String text);

    @Name("Способ оплаты «{{ text }}»")
    @FindBy(".//a[contains(., '{{ text }}')] | " +
            ".//span[.= '{{ text }}'] | " +
            ".//div[@class = 'BillingPaymentMethod' and .= '{{ text }}']")
    VertisElement paymentMethod(@Param("text") String text);

    @Name("Кнопка «Оплатить ...»")
    @FindBy(".//div[contains(@class, 'BillingPaymentMethodsMobile__button')]/button")
    VertisElement payButton();
}