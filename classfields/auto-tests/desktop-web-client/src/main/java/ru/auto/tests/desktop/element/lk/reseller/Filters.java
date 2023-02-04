package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface Filters extends VertisElement, WithInput, WithButton {

    @Name("Переключатель «Графики у всех»")
    @FindBy(".//div[@class = 'SalesFiltersNewDesign__toggle']/label")
    VertisElement showAllChartsToggle();
}
