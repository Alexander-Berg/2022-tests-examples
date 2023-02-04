package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface MoreMenu extends VertisElement {

    @Name("Поделиться")
    @FindBy(".//span[.= 'Поделиться']/span")
    VertisElement share();

}
