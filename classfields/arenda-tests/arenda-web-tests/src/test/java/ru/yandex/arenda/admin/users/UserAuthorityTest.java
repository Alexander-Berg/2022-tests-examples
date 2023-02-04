package ru.yandex.arenda.admin.users;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
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

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.arenda.constants.UriPath.AUTHORITY;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.USER;
import static ru.yandex.arenda.matcher.AttributeMatcher.isChecked;
import static ru.yandex.arenda.matcher.AttributeMatcher.isDisabled;
import static ru.yandex.arenda.pages.AdminUserPage.BIRTHDAY_ID;
import static ru.yandex.arenda.pages.AdminUserPage.BIRTH_PLACE_ID;
import static ru.yandex.arenda.pages.AdminUserPage.CITY_ID;
import static ru.yandex.arenda.pages.AdminUserPage.DEPARTMENT_CODE_ID;
import static ru.yandex.arenda.pages.AdminUserPage.NAME_ID;
import static ru.yandex.arenda.pages.AdminUserPage.NOTARY_FULL_NAME_ID;
import static ru.yandex.arenda.pages.AdminUserPage.PASSPORT_ISSUE_BY_ID;
import static ru.yandex.arenda.pages.AdminUserPage.PASSPORT_ISSUE_DATE_ID;
import static ru.yandex.arenda.pages.AdminUserPage.PASSPORT_SERIES_AND_NUMBER_ID;
import static ru.yandex.arenda.pages.AdminUserPage.PATRONYMIC_ID;
import static ru.yandex.arenda.pages.AdminUserPage.REGISTER_NUMBER_ID;
import static ru.yandex.arenda.pages.AdminUserPage.REGISTRATION_ID;
import static ru.yandex.arenda.pages.AdminUserPage.SAVE_BUTTON;
import static ru.yandex.arenda.pages.AdminUserPage.START_DATE_ID;
import static ru.yandex.arenda.pages.AdminUserPage.SURNAME_ID;
import static ru.yandex.arenda.steps.LkSteps.getNumberedImagePath;
import static ru.yandex.arenda.utils.UtilsWeb.generateRandomCyrillic;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1734")
@DisplayName("[Админка] Доверенность")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserAuthorityTest {

    private static final String EMPTY_STRING = "";
    private static final String TAB_TEXT = "Доверенность";
    private String userId;

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
        String uid = account.getId();
        userId = retrofitApiSteps.getUserId(uid);
    }

    @Test
    @DisplayName("Открыть юзера с доверенностью с пустыми личными данными")
    public void shouldSeeEmptyAuthorityFields() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).path(AUTHORITY).open();
        lkSteps.onAdminUserPage().button(TAB_TEXT).waitUntil(isChecked());
        lkSteps.onAdminUserPage().inputId(REGISTER_NUMBER_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().inputId(NOTARY_FULL_NAME_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().inputId(START_DATE_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().inputId(CITY_ID).should(hasValue(EMPTY_STRING));

        lkSteps.onAdminUserPage().inputId(NAME_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().inputId(SURNAME_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().inputId(PATRONYMIC_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().textAreaId(PASSPORT_ISSUE_BY_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().inputId(DEPARTMENT_CODE_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().inputId(BIRTHDAY_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().textAreaId(BIRTH_PLACE_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onAdminUserPage().textAreaId(REGISTRATION_ID).should(hasValue(EMPTY_STRING));
    }

    @Test
    @DisplayName("Видим ошибки кроме Отчества")
    public void shouldSeeErrorDescription() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).path(AUTHORITY).open();
        lkSteps.onAdminUserPage().button(TAB_TEXT).waitUntil(isChecked());
        lkSteps.onAdminUserPage().button(SAVE_BUTTON).click();
        lkSteps.onAdminUserPage().invalidInput(REGISTER_NUMBER_ID).should(isDisplayed());
        lkSteps.onAdminUserPage().invalidInput(NOTARY_FULL_NAME_ID).should(isDisplayed());
        lkSteps.onAdminUserPage().invalidInput(START_DATE_ID).should(isDisplayed());
        lkSteps.onAdminUserPage().invalidInput(CITY_ID).should(isDisplayed());

        lkSteps.onAdminUserPage().invalidInput(NAME_ID).should(isDisplayed());
        lkSteps.onAdminUserPage().invalidInput(SURNAME_ID).should(isDisplayed());

        lkSteps.onAdminUserPage().invalidInput(PATRONYMIC_ID).should(not(isDisplayed()));

        lkSteps.onAdminUserPage().invalidInput(PASSPORT_SERIES_AND_NUMBER_ID).should(isDisplayed());
        lkSteps.onAdminUserPage().invalidInput(PASSPORT_ISSUE_BY_ID).should(isDisplayed());
        lkSteps.onAdminUserPage().invalidInput(DEPARTMENT_CODE_ID).should(isDisplayed());
        lkSteps.onAdminUserPage().invalidInput(BIRTHDAY_ID).should(isDisplayed());
        lkSteps.onAdminUserPage().invalidInput(BIRTH_PLACE_ID).should(isDisplayed());
        lkSteps.onAdminUserPage().invalidInput(REGISTRATION_ID).should(isDisplayed());
    }

    @Test
    @DisplayName("Скриншотный тест ошибок")
    public void shouldSeeEmptyUserAuthorityScreenShot() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).path(AUTHORITY).open();
        lkSteps.onAdminUserPage().button(TAB_TEXT).waitUntil(isChecked());
        lkSteps.onAdminUserPage().button(SAVE_BUTTON).click();
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onLkPage().root());
        urlSteps.setProductionHost().open();
        lkSteps.onAdminUserPage().button(SAVE_BUTTON).click();
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onLkPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Заполняем поля доверенности, загружаем фото и сохраняем")
    public void shouldSeeFilledFields() {
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).path(AUTHORITY).open();
        lkSteps.onAdminUserPage().button(TAB_TEXT).waitUntil(isChecked());
        final String testString = generateRandomCyrillic(10);
        final String testDate = "01012000";
        lkSteps.onAdminUserPage().inputId(REGISTER_NUMBER_ID).sendKeys(testString);
        lkSteps.onAdminUserPage().inputId(NOTARY_FULL_NAME_ID).sendKeys(testString);
        lkSteps.onAdminUserPage().inputId(START_DATE_ID).sendKeys(testDate);
        lkSteps.onAdminUserPage().inputId(CITY_ID).sendKeys(testString);

        lkSteps.onAdminUserPage().inputId(NAME_ID).sendKeys(testString);
        lkSteps.onAdminUserPage().inputId(SURNAME_ID).sendKeys(testString);
        lkSteps.onAdminUserPage().inputId(PATRONYMIC_ID).sendKeys(testString);
        lkSteps.onAdminUserPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).sendKeys(randomNumeric(10));
        lkSteps.onAdminUserPage().textAreaId(PASSPORT_ISSUE_BY_ID).sendKeys(testString);
        lkSteps.onAdminUserPage().inputId(PASSPORT_ISSUE_DATE_ID).sendKeys(testDate);
        lkSteps.onAdminUserPage().inputId(DEPARTMENT_CODE_ID).sendKeys("020012");
        lkSteps.onAdminUserPage().inputId(BIRTHDAY_ID).sendKeys(testDate);
        lkSteps.onAdminUserPage().textAreaId(BIRTH_PLACE_ID).sendKeys(testString);
        lkSteps.onAdminUserPage().textAreaId(REGISTRATION_ID).sendKeys(testString);
        lkSteps.onAdminUserPage().inputPhoto().sendKeys(getNumberedImagePath(1));
        lkSteps.onAdminUserPage().photosPreviews().should(hasSize(1));
        lkSteps.onAdminUserPage().button(SAVE_BUTTON).click();
        lkSteps.onAdminUserPage().successToast();
    }

    @Test
    @DisplayName("Для забаненных юзеров поля задизейблены (user = 162994608134)")
    public void shouldSeeDisabledFields() {
        final String disabledUserId = "162994608134";
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path("162994608134").path(AUTHORITY).open();
        lkSteps.onAdminUserPage().button(TAB_TEXT).waitUntil(isChecked());
        lkSteps.onAdminUserPage().inputId(REGISTER_NUMBER_ID).should(isDisabled());
        lkSteps.onAdminUserPage().inputId(NOTARY_FULL_NAME_ID).should(isDisabled());
        lkSteps.onAdminUserPage().inputId(START_DATE_ID).should(isDisabled());
        lkSteps.onAdminUserPage().inputId(CITY_ID).should(isDisabled());

        lkSteps.onAdminUserPage().inputId(NAME_ID).should(isDisabled());
        lkSteps.onAdminUserPage().inputId(SURNAME_ID).should(isDisabled());
        lkSteps.onAdminUserPage().inputId(PATRONYMIC_ID).should(isDisabled());
        lkSteps.onAdminUserPage().inputId(PASSPORT_SERIES_AND_NUMBER_ID).should(isDisabled());
        lkSteps.onAdminUserPage().textAreaId(PASSPORT_ISSUE_BY_ID).should(isDisabled());
        lkSteps.onAdminUserPage().inputId(PASSPORT_ISSUE_DATE_ID).should(isDisabled());
        lkSteps.onAdminUserPage().inputId(DEPARTMENT_CODE_ID).should(isDisabled());
        lkSteps.onAdminUserPage().inputId(BIRTHDAY_ID).should(isDisabled());
        lkSteps.onAdminUserPage().textAreaId(BIRTH_PLACE_ID).should(isDisabled());
        lkSteps.onAdminUserPage().textAreaId(REGISTRATION_ID).should(isDisabled());
    }
}
