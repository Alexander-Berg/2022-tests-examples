package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;

public interface BillingPopup extends VertisElement, WithButton, WithCheckbox {

    @Name("Описание")
    @FindBy(".//div[contains(@class, '__description')]")
    VertisElement description();

    @Name("Статус")
    @FindBy(".//div[contains(@class, 'VasPromoItem__footer')]")
    VertisElement status();

    @Name("Кнопка «Подключить за ...»")
    @FindBy(".//div[contains(@class, '-payments')] | " +
            ".//button[contains(@class, 'buyButton')]")
    VertisElement payment();
}
