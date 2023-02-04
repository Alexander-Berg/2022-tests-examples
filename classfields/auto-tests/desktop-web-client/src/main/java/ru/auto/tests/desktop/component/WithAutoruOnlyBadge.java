package ru.auto.tests.desktop.component;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface WithAutoruOnlyBadge {

    @Name("Бейдж «Только на Auto.ru»")
    @FindBy(".//div[contains(@class, 'BadgeForExclusive')]")
    VertisElement autoruOnlyBadge();
}