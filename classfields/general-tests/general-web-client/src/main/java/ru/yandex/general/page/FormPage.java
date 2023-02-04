package ru.yandex.general.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Attribute;
import ru.yandex.general.element.Button;
import ru.yandex.general.element.Checkbox;
import ru.yandex.general.element.FormAddressControl;
import ru.yandex.general.element.FormField;
import ru.yandex.general.element.Input;
import ru.yandex.general.element.InputClear;
import ru.yandex.general.element.Link;
import ru.yandex.general.element.RadioButton;
import ru.yandex.general.element.SuspiciousActivityContacts;

public interface FormPage extends BasePage, Link, Input, Button, Checkbox, RadioButton {

    int FIRST = 0;
    int SECOND = 1;
    String NAZVANIE = "Название";
    String PHOTO = "Фотографии";
    String OPISANIE = "Описание";
    String ZARPLATA = "Зарплата";
    String CONTACTS = "Контакты";
    String ADRES = "Адрес";
    String NEXT = "Дальше";
    String PUBLISH = "Опубликовать";
    String SAVE = "Сохранить";
    String GIVE_FREE = "Отдам даром";
    String ADD_MORE_ADDRESS = "Добавить ещё адрес";
    String DONT_CALL = "Не звоните, пишите в мессенджер";
    String CONTINUE = "Продолжить";
    String START_NEW = "Начать сначала";
    String CHANGE_CATEGORY = "Сменить категорию";
    String NEW_PRODUCT = "Новый товар";
    String USED = "Уже использовался";
    String OFFER_CREATED = "Объявление создано";
    String THANKS = "Ого, спасибо!";
    String VESCHI = "Вещи";
    String RABOTA = "Работа";
    String PARTS = "Транспорт и запчасти";
    String NO_SUITABLE = "Нет подходящей";
    String RUB = "₽";
    String HOW_TO = "Как правильно заполнить";
    String LOGIN = "Войти";
    String REGISTRATION = "Регистрация";
    String SEND_BY_TAXI = "Отправлю на такси или курьером";
    String SEND_RUSSIA = "Отправлю по России";
    String FORM_PAGE_H1 = "Разместить объявление";

    @Name("Поле «{{ value }}»")
    @FindBy("//div[contains(@class, 'FormField__container')][.//span[contains(., '{{ value }}')]]")
    FormField field(@Param("value") String value);

    @Name("Прайс")
    @FindBy("//span[contains(@class, 'FormPriceControl')]//input")
    InputClear price();

    @Name("Список адресов")
    @FindBy("//div[contains(@class, 'FormAddressControl__address')]")
    ElementsCollection<FormAddressControl> addressesList();

    @Name("Список адресов в саджесте")
    @FindBy("//div[contains(@class, 'Popup2_visible Popup2')]//div[contains(@class, 'MenuItem')]")
    ElementsCollection<VertisElement> addressesSuggestList();

    @Name("Список добавленных фото")
    @FindBy("//img[contains(@class, '_thumb_')][not(contains(@class, '_pending_'))]")
    ElementsCollection<VertisElement> photoList();

    @Name("Спиннер")
    @FindBy("//*[contains(@class, 'Spinner')]")
    VertisElement spinner();

    @Name("Состояние товара «{{ value }}»")
    @FindBy("//div[contains(@class, 'FormConditionControlButton__container')][contains(., '{{ value }}')]")
    VertisElement condition(@Param("value") String value);

    @Name("Блок пресетов")
    @FindBy("//div[contains(@class, 'PresetControl__container')]")
    VertisElement presetsBlock();

    @Name("Раздел «{{ value }}»")
    @FindBy("//div[contains(@class, 'PresetButton__button')][contains(., '{{ value }}')]")
    VertisElement section(@Param("value") String value);

    @Name("Ссылка на раздел «{{ value }}»")
    @FindBy("//a[contains(@class, 'PresetButton__button')][contains(., '{{ value }}')]")
    VertisElement sectionLink(@Param("value") String value);

    @Name("Блок выбора категории")
    @FindBy("//div[contains(@class, 'FormCategorySelect')]")
    Link categorySelect();

    @Name("Иконка раскрытия спойлера")
    @FindBy("//*[contains(@class, 'FormContentSpoiler__icon')]")
    VertisElement spoilerOpen();

    @Name("Атрибут «{{ value }}»")
    @FindBy("//div[contains(@class, '_attributeControl_')][./span[.='{{ value }}']]")
    Attribute attribute(@Param("value") String value);

    @Name("Тип связи «{{ value }}» на блоке контактов")
    @FindBy("//div[contains(@class, 'ContactsControlAuthorized__control')][contains(., '{{ value }}')]")
    VertisElement contactType(@Param("value") String value);

    @Name("Блок хлебных крошек")
    @FindBy("//div[contains(@class, 'OfferFormHeader__breadcrumbs')]")
    Link breadcrumbs();

    @Name("Список категорий")
    @FindBy("//div[contains(@class, 'CategorySelectRadio__radio_')]")
    ElementsCollection<VertisElement> categories();

    @Name("Блок паспортной авторизации")
    @FindBy("//div[contains(@class, 'ContactsControlUnauthorized')]")
    Link passportAuthorization();

    @Name("Кнопка «Назад»")
    @FindBy("//span[contains(@class, 'buttonBack')]")
    VertisElement back();

    @Name("Блок контактов с подозрительной активностью ")
    @FindBy("//div[contains(@class, 'ContactsControlSuspicious')]")
    SuspiciousActivityContacts suspiciousActivityContacts();

    @Name("Блок доставки")
    @FindBy("//div[contains(@class, '_deliveryControl')]")
    Checkbox delivery();

    @Name("Список блоков на форме")
    @FindBy("//div[contains(@class, 'FormField__container')]")
    ElementsCollection<VertisElement> formFields();

}
