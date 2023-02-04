package ru.auto.tests.desktop.mobile.element.lk;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 02.02.18
 */

public interface SalesItem extends VertisElement, WithButton {

    String DEACTIVATE_BUTTON = "Снять с продажи";
    String ACTIVATE_BUTTON = "Активировать";
    String OFFER_DEACTIVATED = "Снято с продажи";

    @Name("Название машины")
    @FindBy(".//a[contains(@class, listing-item__link)]")
    VertisElement link();

    @Name("Кнопка «Написать в поддержку»")
    @FindBy(".//a[contains(@class, 'SalesItem__linkChat')]")
    VertisElement supportButton();

    @Name("Статус объявления")
    @FindBy(".//div[contains(@class, 'SalesItemStatus')]")
    VertisElement status();

    @Name("Кнопка «Добавить панораму»")
    @FindBy(".//div[contains(@class, 'PanoramaPromoAddInAppMobile__addButton')]")
    VertisElement addPanoramaButton();
}
