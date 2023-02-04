package ru.auto.tests.desktop.element.lk.reseller;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

public interface MainInfoColumn extends VertisElement, WithButton {

    @Name("Тайтл объявления")
    @FindBy("./a[contains(@class, 'ResellerSalesItemMainInfo__vehicleName')]")
    VertisElement vehicleName();

    @Name("VIN")
    @FindBy(".//div[contains(@class, 'ResellerSalesItemMainInfo__vin')]")
    VertisElement vin();

}
