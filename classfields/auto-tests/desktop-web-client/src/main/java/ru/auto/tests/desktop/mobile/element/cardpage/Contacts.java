package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface Contacts extends VertisElement, WithButton {

    @Name("Плашка «Сейчас дилер не работает»")
    @FindBy("//div[contains(@class, 'SalonSleepPromo')]")
    VertisElement dealerSleepPromo();

    @Name("Плашка «Проверенный дилер»")
    @FindBy("//div[contains(@class, 'salonVerifiedLabel')]")
    VertisElement verifiedDealer();

    @Name("Кол-во офферов продавца")
    @FindBy(".//a[contains(@class, '_offersCount')]")
    VertisElement offersCount();

}
