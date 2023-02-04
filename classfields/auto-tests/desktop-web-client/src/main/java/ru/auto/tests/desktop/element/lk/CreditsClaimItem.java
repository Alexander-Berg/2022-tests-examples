package ru.auto.tests.desktop.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface CreditsClaimItem extends VertisElement, WithButton {

    @Name("Статус")
    @FindBy(".//div[contains(@class, 'CreditClaimSnippetBankDesktop__state')] | " +
            ".//div[contains(@class, 'CreditClaimSnippetBankMobile__state')]")
    VertisElement status();
}
