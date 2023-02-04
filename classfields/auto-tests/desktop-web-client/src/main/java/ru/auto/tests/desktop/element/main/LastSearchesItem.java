package ru.auto.tests.desktop.element.main;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface LastSearchesItem extends VertisElement {

    @Name("Кнопка «Сохранить поиск»")
    @FindBy(".//div[contains(@class, 'IndexSearchHistory__item-button')]")
    VertisElement saveSearchButton();

}
