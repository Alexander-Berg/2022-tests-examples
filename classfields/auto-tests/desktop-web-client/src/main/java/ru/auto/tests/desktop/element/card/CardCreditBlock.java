package ru.auto.tests.desktop.element.card;

import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithInput;

public interface CardCreditBlock extends VertisElement, WithInput, WithButton {

    String CREDIT_CARD_TEXT_UNAUTH = "Подбор кредита в разных банках\nСумма кредита\n1 475 000 ₽\nСрок кредита\n5 лет\nПервый взнос\n20 000 ₽\nПлатеж\nот 28 700 ₽ / мес.\nФИО\nТелефон\nОтправить заявку\nОтправляя заявку, я принимаю условия обработки данных.\nАвтоградъ осуществляет содействие в подборе финансовых услуг.";
    String CREDIT_CARD_TEXT_AUTH = "Подбор кредита в разных банках\nСумма кредита\n1 475 000 ₽\nСрок кредита\n5 лет\nПервый взнос\n20 000 ₽\nПлатеж\nот 28 700 ₽ / мес.\nТелефон\nОтправить заявку\nОтправляя заявку, я принимаю условия обработки данных.\nАвтоградъ осуществляет содействие в подборе финансовых услуг.";
}
