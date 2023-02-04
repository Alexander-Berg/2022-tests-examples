package ru.auto.tests.desktop.element;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 08.11.18
 */
public interface YandexTrustOkPopup extends VertisElement {

    String OK_MESSAGE = "Платеж проведен успешно";

    @Name("Тайтл")
    @FindBy(".//div[@class = 'dialog-inner__title']")
    VertisElement title();

    @Name("Кнопка «ОК»")
    @FindBy(".//div[@class = 'formfield']//button")
    VertisElement ok();
}