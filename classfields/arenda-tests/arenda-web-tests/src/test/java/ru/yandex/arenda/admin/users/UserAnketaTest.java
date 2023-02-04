package ru.yandex.arenda.admin.users;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.constants.UriPath.QUESTIONNAIRE;
import static ru.yandex.arenda.constants.UriPath.USER;
import static ru.yandex.arenda.matcher.AttributeMatcher.isChecked;
import static ru.yandex.arenda.pages.LkPage.ADDITIONAL_TENANT_ID;
import static ru.yandex.arenda.pages.LkPage.PERSONAL_ACTIVITY_TYPE;
import static ru.yandex.arenda.pages.LkPage.PETS_INFO;
import static ru.yandex.arenda.pages.LkPage.ABOUT_WORK_AND_POSITION_ID;
import static ru.yandex.arenda.pages.LkPage.REASON_FOR_RELOCATION_ID;
import static ru.yandex.arenda.pages.LkPage.SEND_BUTTON;
import static ru.yandex.arenda.pages.LkPage.TELL_ABOUT_YOURSELF_ID;
import static ru.yandex.arenda.pages.LkPage.WITH_CHILDREN;
import static ru.yandex.arenda.pages.LkPage.WITH_PETS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1734")
@DisplayName("[Админка] Редактирование анкеты пользователя")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserAnketaTest {

    private static final String VALUE = "value";
    private static final String TENANT_QUESTIONNAIRE = "tenantQuestionnaire";
    private static final String EMPTY_STRING = "";
    private static final String ANKETA = "Анкета";

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

    @Before
    public void before() {
        passportSteps.adminLogin();
    }

    @Test
    @DisplayName("Открываем юзера без анкеты, клик на Сохранить -> поля «Место работы» и «С кем будете » выдают ошибку")
    public void shouldSeeEmptyUserFields() {
        String uid = account.getId();
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());
        lkSteps.onLkPage().activityTenantSelector().should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().textAreaId(REASON_FOR_RELOCATION_ID).should(hasText(EMPTY_STRING));
        lkSteps.onLkPage().textAreaId(PETS_INFO).waitUntil(not(isDisplayed()));
        lkSteps.onLkPage().textAreaId(TELL_ABOUT_YOURSELF_ID).should(hasValue(EMPTY_STRING));
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.onLkPage().invalidInputQuestionnaire(PERSONAL_ACTIVITY_TYPE).should(isDisplayed());
        lkSteps.onLkPage().invalidInputQuestionnaire(ADDITIONAL_TENANT_ID).should(isDisplayed());
    }

    @Test
    @DisplayName("Открываем юзера без анкеты -> заполняем -> клик на сохранить -> success тост ->нет редиректов")
    public void shouldFillEmptyUserFields() {
        String uid = account.getId();
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());
        String fieldText = "field_text";
        lkSteps.onLkPage().activityTenantSelector().click();
        lkSteps.onLkPage().activityTenantSelector().option("Работаю").click();
        lkSteps.onLkPage().textAreaId(ABOUT_WORK_AND_POSITION_ID).sendKeys(fieldText);
        lkSteps.onLkPage().textAreaId(REASON_FOR_RELOCATION_ID).sendKeys(fieldText);
        lkSteps.onLkPage().additionalTenantSelector().click();
        lkSteps.onLkPage().additionalTenantSelector().option("Один").click();
        lkSteps.onLkPage().label(WITH_CHILDREN).click();
        lkSteps.onLkPage().label(WITH_PETS).click();
        lkSteps.onLkPage().textAreaId(PETS_INFO).sendKeys(fieldText);
        lkSteps.onLkPage().textAreaId(TELL_ABOUT_YOURSELF_ID).sendKeys(fieldText);
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.onLkPage().successToast();
        urlSteps.path("/").path("questionnaire").path("/").shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @DisplayName("Юзер с полной анкетой -> удаление всех полей кроме обязательных -> успешное сохранение")
    public void shouldSeeFillUserWithPart() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).path(QUESTIONNAIRE).open();

        lkSteps.clearInputByBackSpace(() -> lkSteps.onLkPage().textAreaId(REASON_FOR_RELOCATION_ID));
        lkSteps.clearInputByBackSpace(() -> lkSteps.onLkPage().textAreaId(PETS_INFO));
        lkSteps.onLkPage().label(WITH_PETS).click();
        lkSteps.onLkPage().label(WITH_CHILDREN).click();
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.onLkPage().successToast();
        urlSteps.path("/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Юзер с полной анкетой. Проверяем поля с бека")
    public void shouldSeeFullUser() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        String userId = retrofitApiSteps.getUserId(uid);
        JsonObject anketa = retrofitApiSteps.getUserByUid(uid).getAsJsonObject(TENANT_QUESTIONNAIRE);
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(USER).path(userId).open();
        lkSteps.onLkPage().button(ANKETA).click();
        lkSteps.onLkPage().button(ANKETA).waitUntil(isChecked());

        String activityActual = lkSteps.onLkPage().activityTenantSelector().getAttribute(VALUE);
        String workActual = lkSteps.onLkPage().textAreaId(ABOUT_WORK_AND_POSITION_ID).getAttribute(VALUE);
        String reasonActual = lkSteps.onLkPage().textAreaId(REASON_FOR_RELOCATION_ID).getText();
        boolean childrenActual = isChecked().matches(lkSteps.onLkPage().label(WITH_CHILDREN));
        boolean petsActual = isChecked().matches(lkSteps.onLkPage().label(WITH_PETS));
        String petsInfoActual = lkSteps.onLkPage().textAreaId(PETS_INFO).getText();
        String aboutActual = lkSteps.onLkPage().textAreaId(TELL_ABOUT_YOURSELF_ID).getText();

        String activityExpected = anketa.getAsJsonObject("personalActivity")
                .getAsJsonPrimitive("activity").getAsString();
        String workExpected = anketa.getAsJsonObject("personalActivity")
                .getAsJsonPrimitive("aboutWorkAndPosition").getAsString();
        String reasonExpected = anketa.getAsJsonPrimitive("reasonForRelocation").getAsString();
        boolean childrenExpected = anketa.getAsJsonPrimitive("hasChildren").getAsBoolean();
        boolean petsExpected = anketa.getAsJsonPrimitive("hasPets").getAsBoolean();
        String petsInfoExpected = anketa.getAsJsonPrimitive("petsInfo").getAsString();
        String aboutExpected = anketa.getAsJsonPrimitive("selfDescription").getAsString();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(activityActual).isEqualTo(activityExpected);
            softly.assertThat(workActual).isEqualTo(workExpected);
            softly.assertThat(reasonActual).isEqualTo(reasonExpected);
            softly.assertThat(childrenActual).isEqualTo(childrenExpected);
            softly.assertThat(petsActual).isEqualTo(petsExpected);
            softly.assertThat(petsInfoActual).isEqualTo(petsInfoExpected);
            softly.assertThat(aboutActual).isEqualTo(aboutExpected);
        });
    }
}
