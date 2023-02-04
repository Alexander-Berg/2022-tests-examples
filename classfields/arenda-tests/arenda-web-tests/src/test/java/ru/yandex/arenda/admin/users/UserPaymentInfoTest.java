package ru.yandex.arenda.admin.users;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.atlas.webdriver.AtlasWebElement;
import org.assertj.core.api.SoftAssertions;
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

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static ru.yandex.arenda.constants.UriPath.CONTRACT;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.LK;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.PAYMENT_DATA;
import static ru.yandex.arenda.constants.UriPath.USER;
import static ru.yandex.arenda.constants.UriPath.USERS;
import static ru.yandex.arenda.matcher.AttributeMatcher.isChecked;
import static ru.yandex.arenda.pages.LkPage.ACCOUNT_NUMBER_ID;
import static ru.yandex.arenda.pages.LkPage.BIK_ID;
import static ru.yandex.arenda.pages.LkPage.INN_ID;
import static ru.yandex.arenda.pages.LkPage.NAME_ID;
import static ru.yandex.arenda.pages.LkPage.PATRONYMIC_ID;
import static ru.yandex.arenda.pages.LkPage.SAVE_BUTTON;
import static ru.yandex.arenda.pages.LkPage.SURNAME_ID;
import static ru.yandex.arenda.pages.LkPaymentDataPage.ACCOUNT_NUMBER_FIELD;
import static ru.yandex.arenda.pages.LkPaymentDataPage.BIK_FIELD;
import static ru.yandex.arenda.pages.LkPaymentDataPage.FIO;
import static ru.yandex.arenda.pages.LkPaymentDataPage.INN_FIELD;
import static ru.yandex.arenda.steps.RetrofitApiSteps.PATH_TO_PATCH_USER;
import static ru.yandex.arenda.utils.UtilsWeb.getObjectFromJson;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1735")
@DisplayName("[Админка] Редактирование платежных данных пользователя")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserPaymentInfoTest {

    private static final String EMPTY_STRING = "";
    private static final String PAYMENT_INFO = "Данные счёта";
    private static final String VALUE = "value";

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
    @DisplayName("Открыть юзера без платежной информации, без ИНН не сохраняется")
    public void shouldSeeEmptyUserFields() {
        String uid = account.getId();
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(PAYMENT_INFO).click();
        lkSteps.onLkPage().button(PAYMENT_INFO).waitUntil(isChecked());
        lkSteps.onLkPage().inputId(SURNAME_ID).sendKeys("вэл");
        lkSteps.onLkPage().button(SAVE_BUTTON).click();
        lkSteps.onLkPage().invalidInputPaymentData(INN_ID).should(isDisplayed());
    }

    @Test
    @DisplayName("Скриншот с ошибками")
    public void shouldSeeErrorFieldsScreenshot() {
        String uid = account.getId();
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(PAYMENT_INFO).click();
        lkSteps.onLkPage().button(PAYMENT_INFO).waitUntil(isChecked());
        fillBadData();

        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onLkPage().root());

        urlSteps.setProductionHost().open();
        lkSteps.onLkPage().button(PAYMENT_INFO).click();
        lkSteps.onLkPage().button(PAYMENT_INFO).waitUntil(isChecked());
        fillBadData();
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onLkPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    private void fillBadData() {
        String BAD_STRING = "2";
        lkSteps.onLkPage().inputId(INN_ID).sendKeys(BAD_STRING);
        lkSteps.onLkPage().inputId(SURNAME_ID).sendKeys(BAD_STRING);
        lkSteps.onLkPage().inputId(NAME_ID).sendKeys(BAD_STRING);
        lkSteps.onLkPage().inputId(PATRONYMIC_ID).sendKeys(BAD_STRING);
        lkSteps.onLkPage().inputId(ACCOUNT_NUMBER_ID).sendKeys(BAD_STRING);
        lkSteps.onLkPage().inputId(BIK_ID).sendKeys(BAD_STRING);
        lkSteps.onLkPage().button(SAVE_BUTTON).click();

        lkSteps.onLkPage().invalidInputPaymentData(INN_ID).should(isDisplayed());
        lkSteps.onLkPage().invalidInputPaymentData(SURNAME_ID).should(isDisplayed());
        lkSteps.onLkPage().invalidInputPaymentData(NAME_ID).should(isDisplayed());
        lkSteps.onLkPage().invalidInputPaymentData(PATRONYMIC_ID).should(isDisplayed());
        lkSteps.onLkPage().invalidInputPaymentData(ACCOUNT_NUMBER_ID).should(isDisplayed());
        lkSteps.onLkPage().invalidInputPaymentData(BIK_ID).should(isDisplayed());
    }

    @Test
    @DisplayName("Юзер привязан к квартире и есть договор, но нет платежных данных")
    public void shouldSeeFullUserWithoutPaymentInfo() {
        String uid = account.getId();
        retrofitApiSteps.getUserByUid(uid);
        JsonObject patch = getObjectFromJson(JsonObject.class, "realty3api/patch_user.json");
        patch.remove("paymentData");
        retrofitApiSteps.patchUserByUid(uid, patch);
        String createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        String contractId = retrofitApiSteps.postModerationFlatContract(createdFlatId);

        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(CONTRACT).path(contractId).path(USERS)
                .open();
        lkSteps.onAdminAssignedUserPage().managerFlatUsersSnippet().userLink().click();
        lkSteps.onLkPage().button(PAYMENT_INFO).click();
        lkSteps.onLkPage().button(PAYMENT_INFO).waitUntil(isChecked());
        lkSteps.onLkPage().inputId(INN_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(SURNAME_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(NAME_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(PATRONYMIC_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(ACCOUNT_NUMBER_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().inputId(BIK_ID).should(hasValue(EMPTY_STRING));
    }

    @Test
    @DisplayName("У юзера заполнены платежные данные. проверяем данные с бека")
    public void shouldSeeFullUserPaymentInfo() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);

        JsonObject paymentInfo = getObjectFromJson(JsonObject.class, PATH_TO_PATCH_USER).getAsJsonObject("paymentData");
        JsonObject person = paymentInfo.getAsJsonObject("person");
        String innExpected = paymentInfo.getAsJsonPrimitive("inn").getAsString();
        String accountNumberExpected = paymentInfo.getAsJsonPrimitive("accountNumber").getAsString();
        String bikExpected = paymentInfo.getAsJsonPrimitive("bik").getAsString();
        String nameExpected = person.getAsJsonPrimitive("name").getAsString();
        String surnameExpected = person.getAsJsonPrimitive("surname").getAsString();
        String patronymicExpected = person.getAsJsonPrimitive("patronymic").getAsString();
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(PAYMENT_INFO).click();
        lkSteps.onLkPage().button(PAYMENT_INFO).waitUntil(isChecked());

        String innActual = lkSteps.onLkPage().inputId(INN_ID).getAttribute(VALUE);
        String accountNumberActual = lkSteps.onLkPage().inputId(ACCOUNT_NUMBER_ID).getAttribute(VALUE);
        String bikActual = lkSteps.onLkPage().inputId(BIK_ID).getAttribute(VALUE);
        String surnameActual = lkSteps.onLkPage().inputId(SURNAME_ID).getAttribute(VALUE);
        String nameActual = lkSteps.onLkPage().inputId(NAME_ID).getAttribute(VALUE);
        String patronymicActual = lkSteps.onLkPage().inputId(PATRONYMIC_ID).getAttribute(VALUE);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(innActual).isEqualTo(innExpected);
            softly.assertThat(bikActual).isEqualTo(bikExpected);
            softly.assertThat(accountNumberActual).isEqualTo(accountNumberExpected);
            softly.assertThat(nameActual).isEqualTo(nameExpected);
            softly.assertThat(surnameActual).isEqualTo(surnameExpected);
            softly.assertThat(patronymicActual).isEqualTo(patronymicExpected);
        });
    }

    @Test
    @DisplayName("Изменить данные юзера привязанного к активному договору -> " +
            "После сохранения данные меняются у юзера и в админке, но не меняются в договоре")
    public void shouldSeeFullUserPaymentInfoEdit() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        String contractId = retrofitApiSteps.postModerationFlatContract(createdFlatId);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(PAYMENT_INFO).click();
        lkSteps.onLkPage().button(PAYMENT_INFO).waitUntil(isChecked());

        String innExpected = "132808730606";
        String accountNumberExpected = randomNumeric(20);
        String bikExpected = "040000000";
        String nameExpected = "Нововалеридзе";
        String surnameExpected = "Нововалеридзе";
        String patronymicExpected = "Нововалерович";
        String fioExpected = format("%s %s %s", nameExpected, surnameExpected, patronymicExpected);

        lkSteps.onLkPage().inputId(INN_ID).clearInputCross().click();
        lkSteps.onLkPage().inputId(ACCOUNT_NUMBER_ID).clearInputCross().click();
        lkSteps.onLkPage().inputId(BIK_ID).clearInputCross().click();
        lkSteps.onLkPage().inputId(NAME_ID).clearInputCross().click();
        lkSteps.onLkPage().inputId(SURNAME_ID).clearInputCross().click();
        lkSteps.onLkPage().inputId(PATRONYMIC_ID).clearInputCross().click();

        lkSteps.onLkPage().inputId(INN_ID).sendKeys(innExpected);
        lkSteps.onLkPage().inputId(ACCOUNT_NUMBER_ID).sendKeys(accountNumberExpected);
        lkSteps.onLkPage().inputId(BIK_ID).sendKeys(bikExpected);
        lkSteps.onLkPage().inputId(NAME_ID).sendKeys(nameExpected);
        lkSteps.onLkPage().inputId(SURNAME_ID).sendKeys(surnameExpected);
        lkSteps.onLkPage().inputId(PATRONYMIC_ID).sendKeys(patronymicExpected);

        lkSteps.onLkPage().button(SAVE_BUTTON).click();
        lkSteps.onLkPage().successToast();

        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(CONTRACT).path(contractId).path(USERS)
                .open();
        passportSteps.logoff();
        passportSteps.login(account);
        urlSteps.testing().path(LK).path(PAYMENT_DATA).open();
        String innActual = lkSteps.onLkPaymentDataPage().field(INN_FIELD).getText();
        String bikActual = lkSteps.onLkPaymentDataPage().field(BIK_FIELD).getText();
        String accountNumberActual = lkSteps.onLkPaymentDataPage().field(ACCOUNT_NUMBER_FIELD).getText();
        String fioActual = lkSteps.onLkPaymentDataPage().field(FIO).getText();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(innActual).contains(innExpected);
            softly.assertThat(bikActual).contains(bikExpected);
            softly.assertThat(accountNumberActual).contains(accountNumberExpected);
            softly.assertThat(fioActual).contains(fioExpected);
        });
    }
}
