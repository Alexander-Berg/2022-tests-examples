package ru.auto.tests.desktop.page.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithNotifier;
import ru.auto.tests.desktop.element.cabinet.manager.CreditSection;

public interface CabinetOrdersCreditPage extends BasePage, WithNotifier, WithButton {

    @Name("Блок «Получайте заявки на покупку авто в кредит»")
    @FindBy("//div[@class = 'ApplicationCredit']")
    CreditSection creditBlock();

    @Name("Поп-ап подтверждения")
    @FindBy("//div[@class = 'ApplicationCredit']")
    VertisElement confirmPopup();
}