package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.common.Button;
import ru.yandex.arenda.element.common.ElementById;
import ru.yandex.arenda.element.common.Input;
import ru.yandex.arenda.element.common.Label;
import ru.yandex.arenda.element.common.Link;
import ru.yandex.arenda.element.common.Select;
import ru.yandex.arenda.element.lk.OwnerFlatSnippet;
import ru.yandex.arenda.element.lk.PersonalDataPhoneModal;
import ru.yandex.arenda.element.lk.PhotoPassportItem;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

public interface LkPage extends BasePage, Button, Input, ElementById, Label, Link {

    String TEST_ADDRESS = "Москва, улица Народного Ополчения, 39к1";
    String TEST_FLAT_NUMBER = "10";
    String TEST_PHONE = "79998883333";
    String TEST_NAME = "Валерий";
    String TEST_SURNAME = "Валеридзе";
    String CONFIRMATION_CODE_NUMBER = "00000";

    String SAVE_DRAFT_AND_CONTINUE = "Сохранить черновик";
    String SEND_BUTTON = "Отправить";
    String CONFIRM_BUTTON = "Подтвердить";
    String SAVE_BUTTON = "Сохранить";
    String ADDRESS_ID = "ADDRESS";
    String FLAT_NUMBER_ID = "FLAT_NUMBER";
    String SURNAME_ID = "SURNAME";
    String NAME_ID = "NAME";
    String PHONE_ID = "PHONE";
    String EMAIL_ID = "EMAIL";
    String PASSPORT_SERIES_AND_NUMBER_ID = "PASSPORT_SERIES_AND_NUMBER";
    String PASSPORT_ISSUE_DATE_ID = "PASSPORT_ISSUE_DATE";
    String PASSPORT_ISSUE_BY_ID = "PASSPORT_ISSUE_BY";
    String DEPARTMENT_CODE_ID = "DEPARTMENT_CODE";
    String BIRTHDAY_ID = "BIRTHDAY";
    String BIRTH_PLACE_ID = "BIRTH_PLACE";
    String PATRONYMIC_ID = "PATRONYMIC";
    String CONFIRMATION_CODE_ID = "CONFIRMATION_CODE";
    String ABOUT_WORK_AND_POSITION_ID = "ABOUT_WORK_AND_POSITION";
    String PETS_INFO = "PETS_INFO";
    String WITH_PETS = "Есть домашние животные";
    String WITH_CHILDREN = "Есть дети";
    String REASON_FOR_RELOCATION_ID = "REASON_FOR_RELOCATION";
    String ADDITIONAL_TENANT_ID = "ADDITIONAL_TENANT";
    String PERSONAL_ACTIVITY_TYPE = "PERSONAL_ACTIVITY_TYPE";
    String TELL_ABOUT_YOURSELF_ID = "TELL_ABOUT_YOURSELF";
    String REGISTRATION_ID = "REGISTRATION";
    String PASS_TO_LK_BUTTON = "Перейти в\u00a0личный кабинет";
    String SEND_TO_CHECK_BUTTON = "Отправить на проверку";
    String SEND_SMS_BUTTON = "Отправить СМС";
    String CONTINUE = "Продолжить";

    String INN_ID = "INN";
    String ACCOUNT_NUMBER_ID = "ACCOUNT_NUMBER";
    String BIK_ID = "BIK";


    @Name("Элемент саджеста «{{ value }}»")
    @FindBy(".//div[contains(@class, 'SuggestList__content')]")
    AtlasWebElement suggestItem(@Param("value") String value);

    @Name("Первый сниппет")
    @FindBy(".//div[@data-test='OwnerFlatSnippet']")
    OwnerFlatSnippet firstOwnerFlatSnippet();

    @Name("Модуль успешно отправленной заявки")
    @FindBy(".//div[@data-test='ArendaModalContent']")
    Button successModal();

    @Name("Модуль успешно отправленной заявки")
    @FindBy(".//div[@data-test='ArendaModalContent']")
    Button confirmModal();

    @Name("Кнопка «Добавить» телефон в заявке")
    @FindBy(".//div[contains(@class,'PersonalDataForm__phoneEditButton') and contains(.,'Добавить')]")
    AtlasWebElement addPhoneButton();

    @Name("Кнопка «Добавить» телефон в заявке")
    @FindBy(".//div[contains(@class,'PersonalDataForm__phoneEditButton') and contains(.,'Изменить')]")
    AtlasWebElement editPhoneButton();

    @Name("Модуль подтверждения телефона")
    @FindBy(".//div[@data-test='UserPersonalDataPhoneModal']")
    PersonalDataPhoneModal personalDataPhoneModal();

    @Name("Селектор «С кем будете проживать»")
    @FindBy(".//select[@id='ADDITIONAL_TENANT']")
    Select additionalTenantSelector();

    @Name("Селектор «Чем занимаетесь»")
    @FindBy(".//select[@id='PERSONAL_ACTIVITY_TYPE']")
    Select activityTenantSelector();

    @Name("Ошибка в поле «{{ value }}» на странице персональных данных")
    @FindBy("//div[contains(@class,'PersonalDataForm__group')][.//*[@id='{{ value }}']]" +
            "//div[contains(@class, 'InputDescription__isInvalid')]/span")
    AtlasWebElement invalidInputPersonalData(@Param("value") String value);

    @Name("Ошибка в поле «{{ value }}» на странице персональных данных")
    @FindBy("//div[contains(@class,'QuestionnaireForm__group')][.//*[@id='{{ value }}']]" +
            "//div[contains(@class, 'InputDescription__isInvalid')]/span")
    AtlasWebElement invalidInputQuestionnaire(@Param("value") String value);

    @Name("Ошибка в поле «{{ value }}» на странице платежных данных")
    @FindBy("//div[contains(@class,'PaymentDataForm__group')][.//*[@id='{{ value }}']]" +
            "//div[contains(@class, 'InputDescription__isInvalid')]/span")
    AtlasWebElement invalidInputPaymentData(@Param("value") String value);

    @Name("Чекбокс согласия на обработку персональных данных")
    @FindBy("//div[contains(@class,'PersonalDataForm__dataProcessingAgreement')]/label")
    AtlasWebElement agreement();

    @Name("Фото-блок загрузки фото паспорта «{{ value }}»")
    @FindBy(".//div[contains(@class, 'FormPassportDocumentsField__item')]")
    ElementsCollection<PhotoPassportItem> photoPassportItems();

    @Name("Врапер если фото паспорта загружено")
    @FindBy(".//div[contains(@class,'DocumentImageUploaderPassportPreview__wrapper')]")
    ElementsCollection<AtlasWebElement> successPhoto();
}
