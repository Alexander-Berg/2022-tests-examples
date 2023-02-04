package ru.auto.tests.desktop.mobile.element.history;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface BoughtReport extends VertisElement, WithButton {

    @Name("Кнопка «Скачать»")
    @FindBy(".//div[contains(@class, 'download')]/button")
    VertisElement downloadButton();

    @Name("Кнопка избранного")
    @FindBy(".//button[contains(@class, '_favoriteButton')]")
    VertisElement favoriteButton();

}