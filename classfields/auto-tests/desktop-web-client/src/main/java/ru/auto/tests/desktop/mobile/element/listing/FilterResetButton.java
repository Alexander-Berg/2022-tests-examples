package ru.auto.tests.desktop.mobile.element.listing;


import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface FilterResetButton extends VertisElement {

    @Name("Крестик (ссылка) сброса фильтра")
    @FindBy(".//a")
    VertisElement resetLink();
}
