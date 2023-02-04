package ru.auto.tests.desktop.mobile.element.cardpage;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface TradeInPopup extends VertisElement, WithButton, WithCheckbox, WithInput {

    @Name("Задизабленная кнопка «Перезвоните мне»")
    @FindBy(".//button[contains(@class, 'Button_type_submit') and contains(@class, 'Button_disabled')]")
    VertisElement callMeButtonDisabled();

    @Name("Цена объявления пользователя")
    @FindBy("(.//div[contains(@class, 'TradeinOffers__descriptionHeader')])[2]")
    VertisElement userSalePrice();

    @Name("Кнопка «Назад» в листалке объявлений")
    @FindBy(".//div[@class = 'TradeinOffers__swiperDotsContainer']/div[contains(@class, 'TradeinOffers__swiperDot')][1]")
    VertisElement prevSaleButton();

    @Name("Кнопка «Вперёд» в листалке объявлений")
    @FindBy(".//div[@class = 'TradeinOffers__swiperDotsContainer']/div[contains(@class, 'TradeinOffers__swiperDot')][2]")
    VertisElement nextSaleButton();
}
