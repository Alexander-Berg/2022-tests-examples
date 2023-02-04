package ru.auto.tests.desktop.mobile.element;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.component.WithCheckbox;
import ru.auto.tests.desktop.mobile.component.WithSelect;

public interface Popup extends VertisElement, WithButton, WithSelect, WithInput, WithSpinner, WithCheckbox {

    @Name("Заголовок")
    @FindBy(".//div[contains(@class, 'VasItemFresh__modalText')]")
    VertisElement title();

    @Name("Иконка закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'modal__close ')] | " +
            ".//div[contains(@class, 'Modal__closer')] |" +
            "..//div[contains(@class, 'Modal__closer')] | " +
            "../..//div[contains(@class, 'Modal__closer')]")
    VertisElement closeIcon();

    @Name("Пункт «{{ text }}»")
    @FindBy(".//div[contains(@class, 'ListItem__name') and .= '{{ text }}'] | " +
            ".//label[.= '{{ text }}']")
    VertisElement item(@Param("text") String text);

    @Name("Причина «{{ text }}»")
    @FindBy(".//div[contains(@class, 'DealCancelPopupItem') and .= '{{ text }}']")
    VertisElement reasonItem(@Param("text") String text);

    @Name("Список телефонов")
    @FindBy(".//div[contains(@class, 'list__phone phone-call__phone-num')]")
    ElementsCollection<VertisElement> phonesList();

    @Name("Кнопка «Написать»")
    @FindBy(".//button[contains(@class, 'chatButton')]")
    VertisElement sendMessageButton();

    @Name("Предложение кредита")
    @FindBy(".//span[contains(@class, 'CreditPrice_type_link')]")
    VertisElement creditOffer();
}
