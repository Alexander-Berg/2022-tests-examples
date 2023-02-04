package ru.yandex.general.mobile.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.mobile.element.Checkbox;
import ru.yandex.general.mobile.element.Input;
import ru.yandex.general.mobile.element.Link;
import ru.yandex.general.mobile.element.NotificationSettings;
import ru.yandex.general.mobile.element.UserInfo;

public interface ContactsPage extends BasePage, Link {

    String NAME = "Имя";
    String LIGHT = "Светлая";
    String DARK = "Тёмная";
    String SYSTEM = "Системная";
    String NOTIFICATIONS = "Уведомления";
    String ADDING_ADDRESS = "Добавление адреса";
    String ADD_ADDRESS = " Добавить адрес";
    String NEWS_AND_ADS = "Новости и рекламные рассылки";
    String UNREAD_CHAT_MESSAGES = "Непрочитанные сообщения в чате";

    @Name("Селектор темы оформления")
    @FindBy("//label[contains(@class, 'SelectButton')]")
    VertisElement colorSchemeSelector();

    @Name("Поле «{{ value }}»")
    @FindBy("//div[contains(@class, 'FormField__container')][.//span[contains(., '{{ value }}')]]")
    Input field(@Param("value") String value);

    @Name("Блок информации о юзере")
    @FindBy("//div[contains(@class, 'PersonalContactsMain__form')][contains(@class, 'root')]")
    UserInfo userInfo();

    @Name("Хэдер")
    @FindBy("//div[contains(@class, 'PageScreen__header_')]")
    VertisElement contactsHeader();

    @Name("Тайтл страницы")
    @FindBy("//span[contains(@class, 'PageScreen__title')]")
    VertisElement pageTitle();

    @Name("Список адресов продажи")
    @FindBy("//div[contains(@class, 'PersonalContactsMain__address_')]//input")
    ElementsCollection<VertisElement> addressesList();

    @Name("Список телефонов")
    @FindBy("//div[contains(@class, 'PersonalContactsMain__phone_')]//input")
    ElementsCollection<VertisElement> phoneList();

    @Name("Настройки нотификаций «{{ value }}»")
    @FindBy("//div[contains(@class, 'NotificationSettings__form')][./span[contains(., '{{ value }}')]]")
    NotificationSettings notificationSettings(@Param("value") String value);


}
