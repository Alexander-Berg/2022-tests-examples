package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface DatesColumn extends VertisElement, WithButton {

    @Name("Дата создания / снятия с продажи")
    @FindBy("./div[contains(@class, 'ResellerSalesItemMainInfo__createDate')]")
    VertisElement createDate();

    @Name("Позиция в поиске")
    @FindBy(".//a[contains(@class, 'ResellerSalesItemSearchPosition')]")
    VertisElement searchPosition();
}
