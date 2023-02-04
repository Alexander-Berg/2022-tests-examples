package ru.auto.tests.desktop.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithRadioButton;

public interface Options extends Block, WithRadioButton, WithCheckbox {

    @Name("Баннер «Мы нашли N доп. опций по вашему VIN»")
    @FindBy(".//div[contains(@class, 'add-options-banner')]")
    AddOptionsBanner addOptionsBanner();

    @Name("Список опций")
    @FindBy(".//div[contains(@class, 'options-checkboxes')]")
    VertisElement list();
}