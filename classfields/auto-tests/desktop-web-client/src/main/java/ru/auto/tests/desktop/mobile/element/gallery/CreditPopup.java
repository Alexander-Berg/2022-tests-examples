package ru.auto.tests.desktop.mobile.element.gallery;

import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.mobile.component.WithButton;

public interface CreditPopup extends VertisElement, WithButton, WithInput {

    String SEND_REQUEST = "Отправить заявку";
    String FIO = "ФИО";
    String PHONE = "Телефон";
    String CODE_FROM_SMS = "Код из смс";

}