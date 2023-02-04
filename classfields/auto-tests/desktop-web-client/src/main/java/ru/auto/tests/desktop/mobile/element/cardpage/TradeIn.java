package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface TradeIn extends VertisElement, WithButton {

    @Name("Поп-ап трейд-ина")
    @FindBy("//div[contains(@class, 'Tradein-module__modal')]//div[@class = 'Modal__content'] | " +
            "//div[contains(@class, 'Modal_visible')]//div[contains(@class, 'Modal__content')]")
    TradeInPopup popup();

}
