package ru.auto.tests.desktop.page;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface BuyoutPage extends BasePage {

    String ESTIMATE_CAR = "Оценить автомобиль";
    String ESTIMATE = "Оценить";
    String PHONE_NUMBER = "Номер телефона";
    String GOSNOMER = "Гос номер или VIN";
    String RUNNING = "Пробег, км";
    String CHECK = "Проверить";
    String SIGNUP = "Записаться";
    String POST_FREE = "Разместить бесплатно";
    String POST = "Разместить объявление";
    String APPLICATION_STATUS = "Посмотреть статус заявки";
    String CHECK_ANOTHER_AUTO = "Оценить другой автомобиль";
    String ACCEPT_RULES = "Принимаю правила и\u00a0даю согласие на\u00a0обработку персональных данных";

    @Name("Плавающая кнопка «Оценить автомобиль»")
    @FindBy("//button[contains(@class, 'C2BAuctionsApplyFormContainer__formButton')]")
    VertisElement floatingBuyoutButton();
}
