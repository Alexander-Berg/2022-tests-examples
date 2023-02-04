package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Alert;
import ru.yandex.realty.element.promocode.PromocodeFeature;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface PromocodePage extends WebPage, Alert {

    String MONEY = "Деньги";
    String PREMIUM = "Премиум";
    String RISING = "Поднятие";
    String SUCCESS_MESSAGE = "Промокод успешно применён";
    String FAILED_MESSAGE = "Не удалось применить промокод";
    String EXPIRE_MESSAGE = "Срок действия промокода истёк";

    @Name("Плашка {{ value }}")
    @FindBy("//div[@class='promocode-item' and contains(.,'{{ value }}')]")
    PromocodeFeature promocodeItem(@Param("value") String value);

    @Name("Поле для ввода промокода")
    @FindBy("//form[@class='promocodes__form']//input")
    AtlasWebElement promoInput();

    @Name("Кнопка «Добавить»")
    @FindBy("//form[@class='promocodes__form']//button")
    AtlasWebElement addPromoButton();
}
