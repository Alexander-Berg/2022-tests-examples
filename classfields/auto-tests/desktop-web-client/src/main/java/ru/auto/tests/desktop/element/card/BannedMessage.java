package ru.auto.tests.desktop.element.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BannedMessage extends VertisElement {

    @Name("Кнопка «Редактировать»")
    @FindBy(".//a[contains(@class, 'BanMessage__Button')]")
    VertisElement editButton();

    @Name("Кнопка «Написать в поддержку»")
    @FindBy(".//button[contains(@class, 'BanMessage__Button')]")
    VertisElement supportButton();
}