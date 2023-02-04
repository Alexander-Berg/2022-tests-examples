package ru.auto.tests.desktop.mobile.element.cardpage;

import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithButton;
import ru.auto.tests.desktop.mobile.element.WithInput;

public interface CardCreditBlock extends VertisElement, WithInput, WithButton {

    String CREDIT_CARD_TEXT_AUTH = "Подбор кредита в разных банках\nСумма кредита\n1 475 000 ₽\n6 лет\n5 лет\n4 года\n3 года\n2 года\n1 год\nПервый взнос\n20 000 ₽\nПлатеж\nот 28 700 ₽ / мес.\nСтоимость автомобиля\n1 495 000 ₽\nТелефон\nОтправить заявку\nОтправляя заявку, я принимаю условия обработки данных. Автоградъ осуществляет содействие в подборе финансовых услуг.";
    String CREDIT_CARD_TEXT_UNAUTH = "Подбор кредита в разных банках\nСумма кредита\n1 475 000 ₽\n6 лет\n5 лет\n4 года\n3 года\n2 года\n1 год\nПервый взнос\n20 000 ₽\nПлатеж\nот 28 700 ₽ / мес.\nСтоимость автомобиля\n1 495 000 ₽\nФИО\nТелефон\nОтправить заявку\nОтправляя заявку, я принимаю условия обработки данных. Автоградъ осуществляет содействие в подборе финансовых услуг.";

}
