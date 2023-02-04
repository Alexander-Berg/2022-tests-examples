package ru.auto.tests.desktop.element.cabinet.header;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 23.04.18
 */
public interface FeedbackPopup extends VertisElement {

    @Name("Закрыть")
    @FindBy("..//div[contains(@class, 'Modal__closer')]")
    VertisElement close();

    @Name("Тема")
    @FindBy("//span[contains(@class, 'TextInput__box') and .//div[ .='Тема']]//input")
    VertisElement subject();

    @Name("Тема сообщения")
    @FindBy(".//textarea")
    VertisElement message();

    @Name("Отправить")
    @FindBy(".//div[contains(@class, 'HeaderFeedbackForm__submit')] //button")
    VertisElement send();

}
