package ru.auto.tests.desktop.element.cabinet.autobidder;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface AttentionSegment extends VertisElement {

    @Name("Каунтер объявлений")
    @FindBy(".//div[contains(@class, '_count')]")
    VertisElement counter();

}
