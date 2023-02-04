package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface Status extends VertisElement, WithButton {

    @Name("Текст статуса")
    @FindBy("./span[contains(@class, 'ResellerSalesItemStatus__text')]")
    VertisElement text();
}
