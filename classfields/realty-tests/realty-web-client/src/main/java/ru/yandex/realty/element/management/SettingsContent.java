package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.RealtyElement;

public interface SettingsContent extends Button, Link, RealtyElement {

    String TITLE_NAME_SECTION = "Название";
    String EMAIL_SECTION = "Эл. почта";
    String YOUR_PHONE_SECTION = "Ваш телефон";
    String PHONE_SECTION = "Телефон";
    String JURIDICAL_ADDRESS_SECTION = "Юр. адрес";
    String ADDRESS_SECTION = "Почтовый адрес";
    String INDEX_SECTION = "Индекс";
    String KPP_SECTION = "КПП";
    String INN_SECTION = "ИНН";
    String SAVE_CHANGES = "Сохранить изменения";
    String NAME_SECTION = "Имя и фамилия";
    String ADD_PHONE = "Добавить";

    @Name("Строка контактов «{{ value }}»")
    @FindBy(".//div[contains(@class,'SettingsContacts__section')][contains(.,'{{ value }}')]")
    SettingsSection section(@Param("value") String value);

    @Name("Строка реквизитов «{{ value }}»")
    @FindBy(".//div[contains(@class,'SettingsRequisitesForm__section')][contains(.,'{{ value }}')]")
    SettingsSection sectionRequisites(@Param("value") String value);

    @Name("Публичный профиль")
    @FindBy(".//div[contains(@class,'SettingsContactsAndProfile__profile')]")
    PublicProfile publicProfile();

    @Name("Блок товарного знака")
    @FindBy(".//div[@id='settings_contacts_trademark']")
    TradeMarkBlock tradeMarkBlock();

    @Name("Сообщение «{{ value }}»")
    @FindBy(".//div[contains(@class, 'form-message__visible')][contains(.,'{{ value }}')]")
    AtlasWebElement message(@Param("value") String value);

    @Name("Блок «mos.ru»")
    @FindBy(".//div[contains(@class,'MosRuSection__container')]")
    AtlasWebElement mosruBLock();
}
