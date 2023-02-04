package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BanPlaceholder extends VertisElement {

    @Name("Причина блокировки")
    @FindBy(".//span[contains(@class, 'OfferSnippetRoyalErrors__reason')]")
    VertisElement reason();
}
