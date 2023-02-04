package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.element.cabinet.PopupBillingBlock;
import ru.auto.tests.desktop.element.cabinet.vin.Vin;

public interface CabinetVinPage extends BasePage, WithNotifier, PopupBillingBlock {

    @Name("Блок VIN")
    @FindBy("//div[contains(@class, 'Layout__twoColumnLeft')]")
    Vin vin();
}