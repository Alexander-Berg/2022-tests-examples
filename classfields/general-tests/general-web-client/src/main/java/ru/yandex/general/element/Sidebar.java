package ru.yandex.general.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface Sidebar extends VertisElement, Button, Link {

    String DELIVERY_RUSSIA = "Отправлю по России";
    String DELIVERY_TAXI = "Отправлю такси или курьером";
    String CHAT_WITH_SUPPORT = "Написать в поддержку";

    @Name("Бейдж цвета «{{ value }}»")
    @FindBy(".//div[contains(@class, '_status_')]/span[contains(@class, 'Badge__{{ value }}')]")
    Badge badge(@Param("value") String value);

    @Name("Бейдж")
    @FindBy(".//span[contains(@class, 'Badge')]")
    Badge badge();

    @Name("Правая кнопка")
    @FindBy(".//button[contains(@class, '_rightBtn')]")
    VertisElement rightButton();

    @Name("Цена")
    @FindBy("//span[contains(@class, '_price')]")
    Link price();

    @Name("Адрес")
    @FindBy(".//div[contains(@class, '_address_')]")
    VertisElement address();

    @Name("Статистика")
    @FindBy("//div[contains(@class, 'Sidebar__statistics')]")
    Statistics statistics();

    @Name("Бейдж доставки")
    @FindBy("//div[contains(@class, '_deliveryBadge')]")
    VertisElement deliveryBadge();

    @Name("Бейдж доставки «{{ value }}»")
    @FindBy("//div[contains(@class, '_deliveryBadge')]//span[contains(., '{{ value }}')]")
    VertisElement deliveryBadge(@Param("value") String value);

    @Name("Информация о продавце")
    @FindBy(".//div[contains(@class, 'CardSeller__container')]")
    SellerInfo sellerInfo();

    @Name("Кнопка «Показать телефон»")
    @FindBy(".//div[contains(@class, '_phone')]//button")
    VertisElement showPhone();

    @Name("Телефон с подменным номером")
    @FindBy(".//div[contains(@class, '_phone')]//button[contains(@class, 'view_green-light')]")
    VertisElement redirectPhone();

    @Name("Кнопка «Написать»")
    @FindBy(".//div[contains(@class, 'ChatButton')]//button")
    VertisElement startChat();

}
