package ru.yandex.arenda.admin.users;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.EDIT;
import static ru.yandex.arenda.constants.UriPath.LK;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.PERSONAL_DATA;
import static ru.yandex.arenda.constants.UriPath.USER;
import static ru.yandex.arenda.matcher.AttributeMatcher.isChecked;
import static ru.yandex.arenda.pages.LkPage.BIRTHDAY_ID;
import static ru.yandex.arenda.pages.LkPage.BIRTH_PLACE_ID;
import static ru.yandex.arenda.pages.LkPage.REGISTRATION_ID;
import static ru.yandex.arenda.pages.LkPage.DEPARTMENT_CODE_ID;
import static ru.yandex.arenda.pages.LkPage.EMAIL_ID;
import static ru.yandex.arenda.pages.LkPage.NAME_ID;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_ISSUE_BY_ID;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_ISSUE_DATE_ID;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_SERIES_AND_NUMBER_ID;
import static ru.yandex.arenda.pages.LkPage.PATRONYMIC_ID;
import static ru.yandex.arenda.pages.LkPage.PHONE_ID;
import static ru.yandex.arenda.pages.LkPage.SAVE_BUTTON;
import static ru.yandex.arenda.pages.LkPage.SURNAME_ID;
import static ru.yandex.arenda.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.arenda.utils.UtilsWeb.makePhoneFormatted;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1733")
@DisplayName("[Админка] Редактирование личных данных пользователя")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserDataTest {

    private static final String VALUE = "value";
    private static final String PERSON = "person";
    private static final String EMPTY_STRING = "";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private Account account;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("Открыть юзера с пустыми личными данными")
    public void shouldSeeEmptyUser() {
        String uid = account.getId();
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button("Личные данные").waitUntil(isChecked());
        lkSteps.onLkPage().inputId(NAME_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(SURNAME_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(PATRONYMIC_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(EMAIL_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(PHONE_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().textAreaId(PASSPORT_ISSUE_BY_ID).should(hasText(EMPTY_STRING));
        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(DEPARTMENT_CODE_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(BIRTHDAY_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().textAreaId(BIRTH_PLACE_ID).should(hasText(EMPTY_STRING));
        lkSteps.onLkPage().textAreaId(REGISTRATION_ID).should(hasText(EMPTY_STRING));
    }

    @Test
    @DisplayName("Пытаемся сохранить пустые поля -> видим ошибки в личных данных, а паспортных нет")
    public void shouldSeeEmptyUserError() {
        String uid = account.getId();
        retrofitApiSteps.getUserByUid(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().inputId(NAME_ID).sendKeys("1");
        lkSteps.onLkPage().button(SAVE_BUTTON).click();
        lkSteps.onLkPage().invalidInputPersonalData(SURNAME_ID).waitUntil(isDisplayed());
        lkSteps.onLkPage().invalidInputPersonalData(PATRONYMIC_ID).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().invalidInputPersonalData(EMAIL_ID).waitUntil(isDisplayed());
        lkSteps.onLkPage().invalidInputPersonalData(PHONE_ID).waitUntil(isDisplayed());
        lkSteps.onLkPage().invalidInputPersonalData(PASSPORT_SERIES_AND_NUMBER_ID).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().invalidInputPersonalData(PASSPORT_ISSUE_BY_ID).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().invalidInputPersonalData(PASSPORT_ISSUE_DATE_ID).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().invalidInputPersonalData(DEPARTMENT_CODE_ID).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().invalidInputPersonalData(BIRTHDAY_ID).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().invalidInputPersonalData(BIRTH_PLACE_ID).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().invalidInputPersonalData(REGISTRATION_ID).waitUntil(not(isDisplayed()));
    }

    @Test
    @DisplayName("Скриншотный тест ошибок")
    public void shouldSeeEmptyUserScreenShot() {
        String uid = account.getId();
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().inputId(NAME_ID).sendKeys("1");
        lkSteps.onLkPage().button(SAVE_BUTTON).click();
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onLkPage().root());
        urlSteps.setProductionHost().open();
        lkSteps.onLkPage().inputId(NAME_ID).sendKeys("1");
        lkSteps.onLkPage().button(SAVE_BUTTON).click();
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onLkPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Видим поля заполненного юзера")
    public void shouldSeeUserData() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        JsonObject user = retrofitApiSteps.getUserByUid(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        String surnameActual = lkSteps.onLkPage().inputId(SURNAME_ID).getAttribute(VALUE);
        String nameActual = lkSteps.onLkPage().inputId(NAME_ID).getAttribute(VALUE);
        String patronymicActual = lkSteps.onLkPage().inputId(PATRONYMIC_ID).getAttribute(VALUE);
        String emailActual = lkSteps.onLkPage().inputId(EMAIL_ID).getAttribute(VALUE);
        String phoneActual = lkSteps.onLkPage().inputId(PHONE_ID).getAttribute(VALUE);

        String surnameExpected = user.getAsJsonObject(PERSON).getAsJsonPrimitive("surname").getAsString();
        String nameExpected = user.getAsJsonObject(PERSON).getAsJsonPrimitive("name").getAsString();
        String patronymicExpected = user.getAsJsonObject(PERSON).getAsJsonPrimitive("patronymic").getAsString();
        String emailExpected = user.getAsJsonPrimitive("email").getAsString();
        String phoneExpected = user.getAsJsonPrimitive("phone").getAsString();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(surnameActual).isEqualTo(surnameExpected);
            softly.assertThat(nameActual).isEqualTo(nameExpected);
            softly.assertThat(patronymicActual).isEqualTo(patronymicExpected);
            softly.assertThat(emailActual).isEqualTo(emailExpected);
            softly.assertThat(phoneActual).isEqualTo(makePhoneFormatted(phoneExpected, PHONE_PATTERN_BRACKETS));
        });
    }

    @Test
    @DisplayName("Удаляем все паспортные данные и сохраняем -> логинимся под юзером и не видим данных")
    public void shouldDeleteUserData() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();

        lkSteps.onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).clearInputCross().click();
        lkSteps.clearInputByBackSpace(() -> lkSteps.onLkPage().textAreaId(PASSPORT_ISSUE_BY_ID),
                isEmptyString(), () -> lkSteps.onLkPage().textAreaId(PASSPORT_ISSUE_BY_ID).getText());
        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).clearInputCross().click();
        lkSteps.onLkPage().inputId(DEPARTMENT_CODE_ID).clearInputCross().click();
        lkSteps.onLkPage().inputId(BIRTHDAY_ID).clearInputCross().click();
        lkSteps.clearInputByBackSpace(() -> lkSteps.onLkPage().textAreaId(BIRTH_PLACE_ID),
                isEmptyString(), () -> lkSteps.onLkPage().textAreaId(BIRTH_PLACE_ID).getText());
        lkSteps.clearInputByBackSpace(() -> lkSteps.onLkPage().textAreaId(REGISTRATION_ID),
                isEmptyString(), () -> lkSteps.onLkPage().textAreaId(REGISTRATION_ID).getText());
        lkSteps.onLkPage().button(SAVE_BUTTON).click();
        lkSteps.onLkPage().successToast();

        passportSteps.logoff();
        passportSteps.login(account);

        urlSteps.testing().path(LK).path(PERSONAL_DATA).path(EDIT).open();
        lkSteps.onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().textAreaId(PASSPORT_ISSUE_BY_ID).should(hasText(EMPTY_STRING));
        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(DEPARTMENT_CODE_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(BIRTHDAY_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().textAreaId(BIRTH_PLACE_ID).should(hasText(EMPTY_STRING));
        lkSteps.onLkPage().textAreaId(REGISTRATION_ID).should(hasText(EMPTY_STRING));
    }

    @Test
    @DisplayName("Изменяем все паспортные данные и сохраняем -> логинимся под юзером и видим новые")
    public void shouldEditUserData() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();


        lkSteps.onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).clearInputCross().click();
        String newPassportSeries = "9876";
        String newPassportNumber = "543210";
        lkSteps.onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).sendKeys(newPassportSeries + newPassportNumber);

        lkSteps.clearInputByBackSpace(() -> lkSteps.onLkPage().textAreaId(PASSPORT_ISSUE_BY_ID),
                isEmptyString(), () -> lkSteps.onLkPage().textAreaId(PASSPORT_ISSUE_BY_ID).getText());
        String newPassportIssueBy = getRandomString();
        lkSteps.onLkPage().textAreaId(PASSPORT_ISSUE_BY_ID).sendKeys(newPassportIssueBy);

        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).clearInputCross().click();
        String newPassportIssueDate = "30.01.2000";
        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).sendKeys(newPassportIssueDate);

        lkSteps.onLkPage().inputId(DEPARTMENT_CODE_ID).clearInputCross().click();
        String newDepartmentCode = "321 - 321";
        lkSteps.onLkPage().inputId(DEPARTMENT_CODE_ID).sendKeys(newDepartmentCode);

        lkSteps.onLkPage().inputId(BIRTHDAY_ID).clearInputCross().click();
        String newBirthday = "01.01.1988";
        lkSteps.onLkPage().inputId(BIRTHDAY_ID).sendKeys(newBirthday);

        lkSteps.clearInputByBackSpace(() -> lkSteps.onLkPage().textAreaId(BIRTH_PLACE_ID),
                isEmptyString(), () -> lkSteps.onLkPage().textAreaId(BIRTH_PLACE_ID).getText());
        String newBirthPlace = getRandomString();
        lkSteps.onLkPage().textAreaId(BIRTH_PLACE_ID).sendKeys(newBirthPlace);

        lkSteps.clearInputByBackSpace(() -> lkSteps.onLkPage().textAreaId(REGISTRATION_ID),
                isEmptyString(), () -> lkSteps.onLkPage().textAreaId(REGISTRATION_ID).getText());
        String newRegAddress = getRandomString();
        lkSteps.onLkPage().textAreaId(REGISTRATION_ID).sendKeys(newRegAddress);

        lkSteps.onLkPage().button(SAVE_BUTTON).click();
        lkSteps.onLkPage().successToast();

        passportSteps.logoff();
        passportSteps.login(account);
        urlSteps.testing().path(LK).path(PERSONAL_DATA).path(EDIT).open();

        // TODO: 15.07.2021 заменить на проверку через бек
        lkSteps.onLkPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID)
                .should(hasValue(format("%s - %s", newPassportSeries, newPassportNumber)));
        lkSteps.onLkPage().textAreaId(PASSPORT_ISSUE_BY_ID).should(hasText(newPassportIssueBy));
        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).should(hasValue(newPassportIssueDate));
        lkSteps.onLkPage().inputId(DEPARTMENT_CODE_ID).should(hasValue(newDepartmentCode));
        lkSteps.onLkPage().inputId(BIRTHDAY_ID).should(hasValue(newBirthday));
        lkSteps.onLkPage().textAreaId(BIRTH_PLACE_ID).should(hasText(newBirthPlace));
        lkSteps.onLkPage().textAreaId(REGISTRATION_ID).should(hasText(newRegAddress));
    }

    @Ignore("Нет такой проверки?")
    @Test
    @DisplayName("Пытаемся ввести дату рождения после выдачи паспорта -> видим ошибку")
    public void shouldSeeUserBirthdayBeforePassport() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();

        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).clearInputCross().click();
        String newPassportIssueDate = "30.01.2000";
        lkSteps.onLkPage().inputId(PASSPORT_ISSUE_DATE_ID).sendKeys(newPassportIssueDate);

        lkSteps.onLkPage().inputId(BIRTHDAY_ID).clearInputCross().click();
        String newBirthday = "31.01.2000";
        lkSteps.onLkPage().inputId(BIRTHDAY_ID).sendKeys(newBirthday);

        lkSteps.onLkPage().invalidInputPersonalData(BIRTHDAY_ID).should(isDisplayed());
        lkSteps.onLkPage().toast().should(not(isDisplayed()));
    }
}
