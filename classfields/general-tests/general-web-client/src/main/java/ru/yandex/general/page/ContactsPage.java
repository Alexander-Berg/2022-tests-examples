package ru.yandex.general.page;

import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.yandex.general.element.Button;
import ru.yandex.general.element.GeneralElement;
import ru.yandex.general.element.InputClear;
import ru.yandex.general.element.Link;
import ru.yandex.general.element.NotificationsRow;

public interface ContactsPage extends BasePage, Link, Button {

    String ADD_ADDRESS = "Добавить адрес";
    String NAME = "Имя";
    String ADDRESSES = "Адреса продажи";
    String SAVE = "Сохранить";
    String LIGHT = "Светлая";
    String DARK = "Тёмная";
    String SYSTEM = "Системная";
    String PROFILE_SETTINGS = "Настройки профиля";
    String NOTIFICATIONS = "Уведомления";
    String NEWS_AND_ADS = "Новости и рекламные рассылки";
    String UNREAD_CHAT_MESSAGES = "Непрочитанные сообщения в чате";
    String PHONE_FOR_YML = "Телефон для YML-фидов";
    String PHONE_FOR_YML_TOOLTIP = "Показывается во всех объявлениях из YML-фида.\nПри загрузке фида по ссылке обновится" +
            "\nавтоматически через несколько часов,\nа для изменения при файловой загрузке\nнеобходимо перезагрузить фид";

    @Name("Поле «{{ value }}»")
    @FindBy("//div[contains(@class, 'FormField__container')][.//span[contains(., '{{ value }}')]]")
    GeneralElement field(@Param("value") String value);

    @Name("Список адресов продажи")
    @FindBy("//div[contains(@class, 'Addresses__address')]//input")
    ElementsCollection<InputClear> addressesList();

    @Name("Список телефонов")
    @FindBy("//div[contains(@class, 'Phones__phone')]//input")
    ElementsCollection<VertisElement> phoneList();

    @Name("Селектор темы оформления")
    @FindBy("//label[contains(@class, 'SelectButton')]")
    VertisElement colorSchemeSelector();

    @Name("Элемент саджеста «{{ value }}»")
    @FindBy("//div[contains(@class, 'MenuItem')][contains(., '{{ value }}')]")
    VertisElement suggestItem(@Param("value") String value);

    @Name("Id юзера")
    @FindBy("//span[contains(@class, 'ContactsForm__userId')]")
    VertisElement userId();

    @Name("Строка в таблице нотификаций «{{ value }}»")
    @FindBy("//div[contains(@class, 'TableItems__row')][contains(., '{{ value }}')]")
    NotificationsRow notificationsTableRow(@Param("value") String value);

    @Name("Иконка информации телефона для YML")
    @FindBy("//div[contains(@class, 'YmlPhone__infoIcon')]")
    VertisElement ymlPhoneInfoIcon();

}
