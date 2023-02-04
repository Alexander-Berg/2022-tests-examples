package ru.auto.tests.desktop.element.cabinet;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 07.12.18
 */

public interface PopupBillingBlock extends VertisElement, WithButton {

    @Name("Содержимое поп-апа")
    @FindBy(".//div[contains(@class, 'BalanceRechargeForm__container')]")
    VertisElement content();

    @Name("Поле ввода суммы")
    @FindBy(".//input[contains(@class, 'TextInput__control')]")
    VertisElement inputSummForBill();

    @Name("Полве ввода email")
    @FindBy(".//div[contains(@class, 'BalanceRechargeForm__emails')]//textarea")
    VertisElement inputEmail();

    @Name("Поле выбора плательщика")
    @FindBy(".//div[contains(@class, 'Select')]")
    VertisElement choicePayer();

    @Name("Чекбокс согласия с офертой")
    @FindBy(".//span[contains(@class, 'Checkbox__checkbox')]")
    VertisElement checkBoxOferta();

    @Name("Кнопка «Выставить счёт»")
    @FindBy(".//button[contains(., 'Выставить счёт')]")
    VertisElement buttonRecharge();

    @Name("Кнопка оплаты картой")
    @FindBy(".//button[contains(., 'Оплата картой')]")
    VertisElement buttonCardPay();

    @Name("Кнопка «{{ name }}»")
    @FindBy(".//button[contains(@class, 'Button')][contains(., '{{ name }}')]")
    VertisElement buttonInBillingBlock(@Param("name") String name);

    @Name("Ссылка на оферту")
    @FindBy(".//a[contains(@class, 'Link')]")
    VertisElement linkOferta();

    @Name("Крест закрытия поп-апа")
    @FindBy(".//div[contains(@class, 'Modal__closer')]")
    VertisElement closePopupIcon();
}
