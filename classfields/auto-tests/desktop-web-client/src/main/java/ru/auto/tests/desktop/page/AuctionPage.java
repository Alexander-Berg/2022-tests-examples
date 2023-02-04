package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.element.auction.MarkBlock;
import ru.auto.tests.desktop.element.auction.ModelBlock;

public interface AuctionPage extends BasePage, WithButton, WithRadioButton {

    @Name("Блок марки")
    @FindBy(".//div[contains(@class, 'MarkFormSection')]")
    MarkBlock markBlock();

    @Name("Блок модели")
    @FindBy(".//div[contains(@class, 'ModelFormSection')]")
    ModelBlock modelBlock();

    @Name("Цвет авто «{{ color }}»")
    @FindBy(".//div[contains(@class, 'ColorItem_{{ color }}')]")
    VertisElement color(@Param("color") String color);

    @Name("Чекбокс добавления авто без ограничений")
    @FindBy(".//span[contains(@class, 'Checkbox__text') and .='Создать заявку без ограничений']")
    VertisElement auctionCheckboxNoLimits();

    @Name("Чекбокс добавления авто на предварительный сбор предложений")
    @FindBy(".//span[contains(@class, 'Checkbox__text') and .='Отправить заявку на предварительный сбор предложений']")
    VertisElement auctionCheckboxPreOffers();

    @Name("Нотификация о добавлении автомобиля в Выкуп")
    @FindBy("//div[contains(@class, 'C2bAuctionAddForm__success') and .='{{ text }}']")
    VertisElement notificationBlock(@Param("text") String text);

    @Name("Блок с сообщением об ошибке добавления автомобиля в Выкуп")
    @FindBy("//div[contains(@class, 'C2bAuctionAddForm__mismatches')]")
    VertisElement errorNotificationBlock();

}