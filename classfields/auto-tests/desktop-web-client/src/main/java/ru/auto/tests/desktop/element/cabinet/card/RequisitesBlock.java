package ru.auto.tests.desktop.element.cabinet.card;

import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithButton;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithInput;
import ru.auto.tests.desktop.component.WithRadioButton;
import ru.auto.tests.desktop.component.WithSelect;

public interface RequisitesBlock extends VertisElement, WithButton, WithInput, WithCheckbox, WithSelect,
        WithRadioButton {

    String ADD_REQUISITES = "Добавить реквизиты";
    String PHYSICAL = "Физлицо";
    String LEGAL_PERSON = "Юрлицо";
    String FULL_NAME = "Полное название";
    String SHORT_NAME = "Краткое название";
    String INN = "ИНН";
    String KPP = "КПП";
    String JURIDICAL_ADDRESS = "Юридический адрес";
    String OGRN = "ОГРН";
    String BIK = "БИК";
    String PAYMENT_ACCOUNT = "Расчетный счет в банке";
    String EMAIL = "Email для счета";
    String CONTACT_PERSON = "Контактное лицо";
    String PHONE_CLASS = "CardDetailsPhone";
    String POSTAL_CODE = "Почтовый индекс";
    String POSTAL_ADDRESS = "Почтовый адрес";
    String NAME = "Имя";
    String SURNAME = "Фамилия";
    String PATRONYMIC = "Отчество";
    String SAVE_CHANGES = "Сохранить изменения";

}
