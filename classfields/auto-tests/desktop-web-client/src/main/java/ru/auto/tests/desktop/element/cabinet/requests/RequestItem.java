package ru.auto.tests.desktop.element.cabinet.requests;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface RequestItem extends VertisElement {

    @Name("Название (марка, модель)")
    @FindBy(".//span[@class = 'Link']")
    VertisElement title();
}