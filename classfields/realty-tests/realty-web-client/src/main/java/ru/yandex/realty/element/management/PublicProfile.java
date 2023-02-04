package ru.yandex.realty.element.management;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Button;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

public interface PublicProfile extends Button {

    String ENABLE_PROFILE_WARN = "Включите профиль, чтобы он появился на сервисе";
    String ADD_LOGO_WARN = "Добавьте логотип";
    String ADD_PHOTO_WARN = "Добавьте фотографию";
    String FILL_FOUND_DATE_WARN = "Заполните дату основания";
    String FILL_FIELD = "Заполните поле";
    String FILL_ADDRESS_WARN = "Укажите адрес";
    String FILL_WORK_DAYS_WARN = "Укажите дни работы";
    String FILL_WORK_TIME_WARN = "Укажите время работы";
    String FILL_FIELDS = "Заполните поля";
    String FILL_DESCRIPTION_WARN = "Заполните описание";
    String FILE_OVER_10MB = "Файл больше 10Мб";

    @Name("Строка «{{ value }}»")
    @FindBy(".//div[contains(@class,'SettingsContacts__section')][contains(.,'{{ value }}')]")
    ProfileSection profileSection(@Param("value") String value);

    @Name("Описание")
    @FindBy(".//textarea")
    AtlasWebElement descriptionArea();

    @Name("Сообщение «{{ value }}»")
    @FindBy(".//div[contains(@class, 'form-message__visible')][contains(.,'{{ value }}')]")
    AtlasWebElement message(@Param("value") String value);

    @Name("Кнопка включение/выключения")
    @FindBy(".//div[contains(@class,'Tumbler_view_yellow')]")
    AtlasWebElement tumblerButton();

    default void enableProfile() {
        tumblerButton().should(not(isChecked())).click();
        tumblerButton().should(isChecked());
    }
}
