package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.common.ElementById;
import ru.yandex.arenda.element.common.Input;
import ru.yandex.arenda.element.lk.ownerlk.PhotosPreview;

public interface AdminUserPage extends BasePage, ElementById, Button, Input {

    String CONCIERGE_COMMENT_ID = "CONCIERGE_COMMENT";
    String SAVE_BUTTON = "Сохранить";
    String REGISTER_NUMBER_ID = "REGISTER_NUMBER";
    String NOTARY_FULL_NAME_ID = "NOTARY_FULL_NAME";
    String START_DATE_ID = "START_DATE";
    String CITY_ID = "CITY";
    String SURNAME_ID = "SURNAME";
    String NAME_ID = "NAME";
    String PATRONYMIC_ID = "PATRONYMIC";
    String PASSPORT_SERIES_AND_NUMBER_ID = "PASSPORT_SERIES_AND_NUMBER";
    String PASSPORT_ISSUE_BY_ID = "PASSPORT_ISSUE_BY";
    String PASSPORT_ISSUE_DATE_ID = "PASSPORT_ISSUE_DATE";
    String DEPARTMENT_CODE_ID = "DEPARTMENT_CODE";
    String BIRTHDAY_ID = "BIRTHDAY";
    String BIRTH_PLACE_ID = "BIRTH_PLACE";
    String REGISTRATION_ID = "REGISTRATION";

    @Name("Ошибка в поле «{{ value }}» на странице персональных данных")
    @FindBy("//div[contains(@class,'withManagerFormClasses__group')][.//*[@id='{{ value }}']]" +
            "//div[contains(@class, 'withManagerFormClasses__description')]/span")
    AtlasWebElement invalidInput(@Param("value") String value);

    @Name("Инпут загрузки фото")
    @FindBy(".//input[@type = 'file']")
    AtlasWebElement inputPhoto();

    @Name("Превьюшки фото")
    @FindBy(".//div[contains(@class,'ImageUploaderImagePreview__wrapper')]")
    ElementsCollection<PhotosPreview> photosPreviews();
}
