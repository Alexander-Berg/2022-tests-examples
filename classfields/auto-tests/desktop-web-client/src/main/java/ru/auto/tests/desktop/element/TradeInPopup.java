package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithInput;

public interface TradeInPopup extends VertisElement, WithInput, WithButton, WithCheckbox {

    @Name("Цена объявления пользователя")
    @FindBy("(.//div[contains(@class, 'TradeinOffers__descriptionHeader')])[2]")
    VertisElement userSalePrice();

    @Name("Кнопка «Назад» в листалке объявлений")
    @FindBy(".//button[@title = 'Назад']")
    VertisElement prevSaleButton();

    @Name("Кнопка «Вперёд» в листалке объявлений")
    @FindBy(".//button[@title = 'Вперед']")
    VertisElement nextSaleButton();
}
