package ru.yandex.arenda.steps;

import io.qameta.allure.Step;
import org.hamcrest.Matchers;
import org.openqa.selenium.Keys;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.element.lk.admin.StepModal.DONE_BUTTON;
import static ru.yandex.arenda.element.lk.admin.StepModal.NEXT_BUTTON;
import static ru.yandex.arenda.pages.AdminAssignedUserPage.USER_SUGGEST_ID;
import static ru.yandex.arenda.pages.AdminFlatPage.FLAT_ADDRESS;
import static ru.yandex.arenda.pages.ContractPage.RENT_AMOUNT_ID;
import static ru.yandex.arenda.pages.ContractPage.RENT_PAYMENT_DAY_OF_MONTH_ID;
import static ru.yandex.arenda.pages.ContractPage.RENT_START_DATE_ID;
import static ru.yandex.arenda.pages.ContractPage.TENANT_EMAIL_ID;
import static ru.yandex.arenda.pages.ContractPage.TENANT_NAME_ID;
import static ru.yandex.arenda.pages.ContractPage.TENANT_PATRONYMIC_ID;
import static ru.yandex.arenda.pages.ContractPage.TENANT_PHONE_ID;
import static ru.yandex.arenda.pages.ContractPage.TENANT_SURNAME_ID;
import static ru.yandex.arenda.pages.LkPage.ADDRESS_ID;
import static ru.yandex.arenda.pages.LkPage.BIRTHDAY_ID;
import static ru.yandex.arenda.pages.LkPage.BIRTH_PLACE_ID;
import static ru.yandex.arenda.pages.LkPage.CONFIRMATION_CODE_ID;
import static ru.yandex.arenda.pages.LkPage.CONFIRMATION_CODE_NUMBER;
import static ru.yandex.arenda.pages.LkPage.CONFIRM_BUTTON;
import static ru.yandex.arenda.pages.LkPage.DEPARTMENT_CODE_ID;
import static ru.yandex.arenda.pages.LkPage.FLAT_NUMBER_ID;
import static ru.yandex.arenda.pages.LkPage.NAME_ID;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_ISSUE_BY_ID;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_ISSUE_DATE_ID;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_SERIES_AND_NUMBER_ID;
import static ru.yandex.arenda.pages.LkPage.PATRONYMIC_ID;
import static ru.yandex.arenda.pages.LkPage.PHONE_ID;
import static ru.yandex.arenda.pages.LkPage.ABOUT_WORK_AND_POSITION_ID;
import static ru.yandex.arenda.pages.LkPage.REASON_FOR_RELOCATION_ID;
import static ru.yandex.arenda.pages.LkPage.SEND_SMS_BUTTON;
import static ru.yandex.arenda.pages.LkPage.SURNAME_ID;
import static ru.yandex.arenda.pages.LkPage.TELL_ABOUT_YOURSELF_ID;
import static ru.yandex.arenda.pages.LkPage.REGISTRATION_ID;
import static ru.yandex.arenda.pages.LkPage.TEST_FLAT_NUMBER;
import static ru.yandex.arenda.pages.LkPage.TEST_PHONE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class LkSteps extends MainSteps {

    @Step("Заполняем поля для отправки заявки на квартиру")
    public void fillFormApplication(String address, String name, String surname, String phone) {

        onLkPage().inputId(ADDRESS_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(ADDRESS_ID).sendKeys(address);
        onLkPage().suggestItem(address).click();
        onLkPage().inputId(FLAT_NUMBER_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(FLAT_NUMBER_ID).sendKeys(TEST_FLAT_NUMBER);
        onLkPage().inputId(NAME_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(NAME_ID).sendKeys(name);
        onLkPage().inputId(SURNAME_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(SURNAME_ID).sendKeys(surname);
        onLkPage().inputId(PHONE_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(PHONE_ID).sendKeys(phone);
    }

    @Step("Заполняем поля персональных данных")
    public void fillFormPersonalData(String name, String surname, String patronomyc) {
        onLkPage().inputId(NAME_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(NAME_ID).sendKeys(name);
        onLkPage().inputId(SURNAME_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(SURNAME_ID).sendKeys(surname);
        onLkPage().inputId(PATRONYMIC_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(PATRONYMIC_ID).sendKeys(patronomyc);
    }

    @Step("Заполняем поля паспортных данных")
    public void fillFormPassportData(String passport, String passportIssue, String date, String departmentCode,
                                     String birthday, String birthPlace) {
        onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).sendKeys(passport);
        onLkPage().textAreaId(PASSPORT_ISSUE_BY_ID).sendKeys(passportIssue);
        onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).sendKeys(date);
        onLkPage().inputId(DEPARTMENT_CODE_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(DEPARTMENT_CODE_ID).sendKeys(departmentCode);
        onLkPage().inputId(BIRTHDAY_ID).clearInputCross().clickIf(isDisplayed());
        onLkPage().inputId(BIRTHDAY_ID).sendKeys(birthday);
        onLkPage().textAreaId(BIRTH_PLACE_ID).sendKeys(birthPlace);
        onLkPage().textAreaId(REGISTRATION_ID).sendKeys(getRandomString());
        onLkPage().agreement().click();
    }

    @Step("Заполняем поле телефона и подтверждаем")
    public void fillPhonePersonalData() {
        onLkPage().addPhoneButton().click();
        onLkPage().personalDataPhoneModal().inputId(PHONE_ID).sendKeys(TEST_PHONE);
        onLkPage().personalDataPhoneModal().button(SEND_SMS_BUTTON).click();
        onLkPage().personalDataPhoneModal().inputId(CONFIRMATION_CODE_ID).sendKeys(CONFIRMATION_CODE_NUMBER);
        onLkPage().personalDataPhoneModal().button(CONFIRM_BUTTON).click();
    }

    @Step("Заполняем поля анкеты жильца")
    public void fillAnketa() {
        onLkPage().activityTenantSelector().click();
        onLkPage().activityTenantSelector().option("Работаю").click();
        onLkPage().textAreaId(ABOUT_WORK_AND_POSITION_ID).sendKeys(getRandomString());
//        onLkPage().inputId(SOCIAL_NETWORK_LINK_ID).sendKeys("https://arenda.yandex.ru/");
        onLkPage().textAreaId(REASON_FOR_RELOCATION_ID).sendKeys(getRandomString());
        onLkPage().textAreaId(TELL_ABOUT_YOURSELF_ID).sendKeys(getRandomString());
        onLkPage().additionalTenantSelector().click();
        onLkPage().additionalTenantSelector().option("Один").click();
    }

    @Step("Заполняем поля для создания квартиры в админке. Десктоп")
    public void fillFieldsForCreateFlatDesktop(String address) {
        onAdminFlatPage().byId(FLAT_ADDRESS).sendKeys(address);
        onAdminFlatPage().suggestList().waitUntil(hasSize(greaterThan(0))).get(FIRST).click();
    }

    @Step("Заполняем поля для создания квартиры в админке. Тач")
    public void fillFieldsForCreateFlatTouch(String address) {
        onAdminFlatPage().byId(FLAT_ADDRESS).click();
        onAdminFlatPage().stepModal().byId(FLAT_ADDRESS).sendKeys(address);
        onAdminFlatPage().suggestList().waitUntil(hasSize(greaterThan(0))).get(FIRST).click();
        onAdminFlatPage().stepModal().button(DONE_BUTTON).click();
    }

    @Step("Заполняем поля для создания договора в админке")
    public void fillFieldsForCreateContract(String rentAmount,
                                            String rentStartDate,
                                            String rentPaymentDayOfMonth,
                                            String insurancePolicyAmount,
                                            String ownerName,
                                            String ownerSurname,
                                            String ownerPatronymic,
                                            String ownerPhone,
                                            String ownerEmail,
                                            String ownerBankAccountNumber,
                                            String ownerBankAccountBik,
                                            String ownerInn,
                                            String tenantName,
                                            String tenantSurname,
                                            String tenantPatronymic,
                                            String tenantPhone,
                                            String tenantEmail) {
        onContractPage().byId(RENT_AMOUNT_ID).sendKeys(rentAmount);
        onContractPage().inputId(RENT_START_DATE_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().byId(RENT_START_DATE_ID).sendKeys(rentStartDate);
        onContractPage().inputId(RENT_PAYMENT_DAY_OF_MONTH_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().byId(RENT_PAYMENT_DAY_OF_MONTH_ID).sendKeys(rentPaymentDayOfMonth);


//        onContractPage().byId(OWNER_NAME_ID).sendKeys(ownerName);
//        onContractPage().byId(OWNER_SURNAME_ID).sendKeys(ownerSurname);
//        onContractPage().byId(OWNER_PATRONYMIC_ID).sendKeys(ownerPatronymic);
//        onContractPage().byId(OWNER_PHONE_ID).sendKeys(ownerPhone);
//        onContractPage().byId(OWNER_EMAIL_ID).sendKeys(ownerEmail);
//        onContractPage().byId(OWNER_BANK_ACCOUNT_NUMBER_ID).sendKeys(ownerBankAccountNumber);
//        onContractPage().byId(OWNER_BANK_ACCOUNT_BIK_ID).sendKeys(ownerBankAccountBik);
//        onContractPage().byId(OWNER_INN_ID).sendKeys(ownerInn);

        onContractPage().inputId(TENANT_NAME_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().inputId(TENANT_NAME_ID).sendKeys(tenantName);
        onContractPage().inputId(TENANT_SURNAME_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().inputId(TENANT_SURNAME_ID).sendKeys(tenantSurname);
        onContractPage().inputId(TENANT_PATRONYMIC_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().inputId(TENANT_PATRONYMIC_ID).sendKeys(tenantPatronymic);
        onContractPage().inputId(TENANT_PHONE_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().inputId(TENANT_PHONE_ID).sendKeys(tenantPhone);
        onContractPage().inputId(TENANT_EMAIL_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().inputId(TENANT_EMAIL_ID).sendKeys(tenantEmail);
    }

    @Step("Заполняем поля для создания договора в админке")
    public void fillFieldsForCreateContractTouch(String rentAmount,
                                                 String rentStartDate,
                                                 String rentPaymentDayOfMonth,
                                                 String ownerName,
                                                 String ownerSurname,
                                                 String ownerPatronymic,
                                                 String ownerPhone,
                                                 String ownerEmail,
                                                 String ownerBankAccountNumber,
                                                 String ownerBankAccountBik,
                                                 String ownerInn,
                                                 String tenantName,
                                                 String tenantSurname,
                                                 String tenantPatronymic,
                                                 String tenantPhone,
                                                 String tenantEmail) {
        onContractPage().byId(RENT_AMOUNT_ID).click();
        onContractPage().stepModal().byId(RENT_AMOUNT_ID).sendKeys(rentAmount);
        onContractPage().stepModal().button(NEXT_BUTTON).click();
        onContractPage().stepModal().inputId(RENT_START_DATE_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().stepModal().byId(RENT_START_DATE_ID).sendKeys(rentStartDate);
        onContractPage().stepModal().button(NEXT_BUTTON).click();
        onContractPage().stepModal().inputId(RENT_PAYMENT_DAY_OF_MONTH_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().stepModal().byId(RENT_PAYMENT_DAY_OF_MONTH_ID).sendKeys(rentPaymentDayOfMonth);
        onContractPage().stepModal().button(NEXT_BUTTON).click();
        onContractPage().stepModal().button(NEXT_BUTTON).click();
        onContractPage().stepModal().button(NEXT_BUTTON).click();
        onContractPage().stepModal().button(DONE_BUTTON).click();

//        onContractPage().byId(OWNER_NAME_ID).click();
//        onContractPage().stepModal().byId(OWNER_NAME_ID).sendKeys(ownerName);
//        onContractPage().stepModal().button(NEXT_BUTTON).click();
//        onContractPage().stepModal().byId(OWNER_SURNAME_ID).sendKeys(ownerSurname);
//        onContractPage().stepModal().button(NEXT_BUTTON).click();
//        onContractPage().stepModal().byId(OWNER_PATRONYMIC_ID).sendKeys(ownerPatronymic);
//        onContractPage().stepModal().button(NEXT_BUTTON).click();
//        onContractPage().stepModal().byId(OWNER_PHONE_ID).sendKeys(ownerPhone);
//        onContractPage().stepModal().button(NEXT_BUTTON).click();
//        onContractPage().stepModal().byId(OWNER_EMAIL_ID).sendKeys(ownerEmail);
//        onContractPage().stepModal().button(NEXT_BUTTON).click();
//        onContractPage().stepModal().byId(OWNER_BANK_ACCOUNT_NUMBER_ID).sendKeys(ownerBankAccountNumber);
//        onContractPage().stepModal().button(NEXT_BUTTON).click();
//        onContractPage().stepModal().byId(OWNER_BANK_ACCOUNT_BIK_ID).sendKeys(ownerBankAccountBik);
//        onContractPage().stepModal().button(NEXT_BUTTON).click();
//        onContractPage().stepModal().byId(OWNER_INN_ID).sendKeys(ownerInn);
//        onContractPage().stepModal().button(DONE_BUTTON).click();

        onContractPage().byId(TENANT_NAME_ID).click();
        onContractPage().stepModal().inputId(TENANT_NAME_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().stepModal().inputId(TENANT_NAME_ID).sendKeys(tenantName);
        onContractPage().stepModal().button(NEXT_BUTTON).click();
        onContractPage().stepModal().inputId(TENANT_SURNAME_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().stepModal().inputId(TENANT_SURNAME_ID).sendKeys(tenantSurname);
        onContractPage().stepModal().button(NEXT_BUTTON).click();
        onContractPage().stepModal().inputId(TENANT_PATRONYMIC_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().stepModal().inputId(TENANT_PATRONYMIC_ID).sendKeys(tenantPatronymic);
        onContractPage().stepModal().button(NEXT_BUTTON).click();
        onContractPage().stepModal().inputId(TENANT_PHONE_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().stepModal().inputId(TENANT_PHONE_ID).sendKeys(tenantPhone);
        onContractPage().stepModal().button(NEXT_BUTTON).click();
        onContractPage().stepModal().inputId(TENANT_EMAIL_ID).clearInputCross().clickIf(isDisplayed());
        onContractPage().stepModal().inputId(TENANT_EMAIL_ID).sendKeys(tenantEmail);
        onContractPage().stepModal().button(DONE_BUTTON).click();
    }

    @Step("Получаем ссылку на привязку пользователя")
    public String getAssignLink() {
        onAdminAssignedUserPage().copyLinkButton().click();
        onAdminAssignedUserPage().successToast();
        // TODO: 15.07.2021
        //вот такой вот костыль на маках надо использовать Keys.COMMAND
        scrollElementToCenter(onAdminAssignedUserPage().inputId(USER_SUGGEST_ID));
        onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).click();
        onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).sendKeys(Keys.CONTROL + "v");
        return onAdminAssignedUserPage().inputId(USER_SUGGEST_ID).getAttribute("value");
        //код ниже работает только локально
//        try {
//            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
//        } catch (Exception e) {
//            throw new RuntimeException("Не удалось получить ссылку" + e);
//        }
    }

    public void setFileDetector() {
        if (!config.isLocalDebug()) {
            ((RemoteWebDriver) getDriver()).setFileDetector(new LocalFileDetector());
        }
    }

    @Step("Добавляем фото со страницы списка оферов")
    public void addPhoto() {
        setFileDetector();
        onLkOwnerFlatPhotoPage().photoInput().sendKeys(getDefaultImagePath());
    }

    public static String getDefaultImagePath() {
        return new File("src/test/resources/offer/offer_img.jpg").getAbsolutePath();
    }

    @Step("Добавляем фото номер {number} к объявлению")
    public void addPhotoNumber(int number) {
        setFileDetector();
        onLkOwnerFlatPhotoPage().photoInput().sendKeys(getNumberedImagePath(number));
    }

    public static String getNumberedImagePath(int number) {
        return new File(format("target/test-classes/offer/offer_photo_%d.jpg", number)).getAbsolutePath();
    }

    @Step("Добавляем {number}")
    public void addPhotoCount(int number) {
        setFileDetector();
        for (int i = 0; i < number; i++) {
            // TODO: 08.10.2021 шаг нужен потому что в инпут почему-то загружается еще и предыдущий файл
            onLkOwnerFlatPhotoPage().photoInput().executeScript("arguments[0].value = '';");
            onLkOwnerFlatPhotoPage().photoInput().sendKeys(getNumberedImagePath((i % 4) + 1));
            onLkOwnerFlatPhotoPage().photosPreviews().waitUntil(Matchers.hasSize(i + 1));
        }
    }
}
