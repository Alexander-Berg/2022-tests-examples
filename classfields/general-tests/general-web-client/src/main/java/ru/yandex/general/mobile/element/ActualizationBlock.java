package ru.yandex.general.mobile.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

public interface ActualizationBlock extends VertisElement, Link {

    String YES = "Да";
    String NO = "Нет";
    String IS_ACTUAL = "Ещё актуально?";
    String OFFER_WILL_RAISE = "Объявление поднимется в поиске!";

    @Name("Текст")
    @FindBy("./span")
    VertisElement text();

    @Name("Кнопки действий")
    @FindBy(".//div[contains(@class, 'IsExpired__btnWrapper')]")
    Link buttons();

}
