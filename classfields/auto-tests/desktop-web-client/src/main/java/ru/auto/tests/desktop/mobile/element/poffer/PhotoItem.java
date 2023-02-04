package ru.auto.tests.desktop.mobile.element.poffer;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface PhotoItem extends VertisElement {

    @Name("Лоадер")
    @FindBy(".//div[@class = 'MdsPhoto__loader']")
    VertisElement loader();

}
