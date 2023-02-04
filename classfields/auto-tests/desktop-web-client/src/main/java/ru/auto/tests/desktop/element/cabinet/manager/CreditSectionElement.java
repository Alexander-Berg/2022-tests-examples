package ru.auto.tests.desktop.element.cabinet.manager;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface CreditSectionElement extends VertisElement, WithButton, WithInput {

    @Name("Нажатый тумблер «{{ text }}»")
    @FindBy("//div[contains(@class, 'DealerCreditSwitch__itemRightBlock') and contains(., '{{ text }}')]" +
            "//label[contains(@class, 'Toggle_checked')]")
    VertisElement pressedToggle(@Param("text") String text);

    @Name("Ненажатый тумблер «{{ text }}»")
    @FindBy("//div[contains(@class, 'DealerCreditSwitch__itemRightBlock') and contains(., '{{ text }}')]" +
            "//label[not(contains(@class, 'Toggle_checked'))]")
    VertisElement notPressedToggle(@Param("text") String text);

    @Name("Нажатый тумблер с ожиданием подтверждения «{{ text }}»")
    @FindBy("//div[contains(@class, 'DealerCreditSwitch__itemRightBlock') and contains(., '{{ text }}')]" +
            "//label[contains(@class, 'Toggle_checked Toggle_disabled')]")
    VertisElement pressedDisabledToggle(@Param("text") String text);

    @Name("Слайдер срока кредита")
    @FindBy(".//div[contains(@class, 'Slider_type_double')]")
    VertisElement periodSlider();
}