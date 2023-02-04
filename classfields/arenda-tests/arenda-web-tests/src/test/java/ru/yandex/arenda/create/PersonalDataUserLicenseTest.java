package ru.yandex.arenda.create;

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
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.constants.UriPath.LK_TENANT_QUESTIONNAIRE_EDIT;
import static ru.yandex.arenda.pages.LkPage.SEND_BUTTON;
import static ru.yandex.arenda.pages.LkPage.SEND_TO_CHECK_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1724")
@DisplayName("Личные данные")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class PersonalDataUserLicenseTest {

    private static final String PASSPORT_TEXT = "9999999999";
    private static final String PASSPORT_ISSUE_BY = getRandomString();
    private static final String ISSUE_DATE_TEXT = "01012000";
    private static final String DEPARTMENT_TEXT = "123 - 456";
    private static final String BIRTHDAY_TEXT = "01011990";
    private static final String BIRTHPLACE_TEXT = getRandomString();
    private static final String NAME = "Валерий";
    private static final String SURNAME = "Валеридзе";
    private static final String PATRONYMIC = "Валерыч";
    private static final String TERMS_TEXT = "Условия оказания услуг";
    private static final String USER_LICENSE_TEXT = "Пользовательского соглашения";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
        lkSteps.onLkPage().link(USER_LICENSE_TEXT).should(isDisplayed());
        lkSteps.fillFormPersonalData(NAME, SURNAME, PATRONYMIC);
        lkSteps.fillPhonePersonalData();
        lkSteps.fillFormPassportData(PASSPORT_TEXT, PASSPORT_ISSUE_BY, ISSUE_DATE_TEXT, DEPARTMENT_TEXT, BIRTHDAY_TEXT,
                BIRTHPLACE_TEXT);
        lkSteps.onLkPage().editPhoneButton().waitUntil(isDisplayed());
        lkSteps.onLkPage().button(SEND_TO_CHECK_BUTTON).click();
    }

    @Test
    @DisplayName("Юзер новый, без заявок и личных данных. Соглашения в разных местах, до и после заполнения")
    public void shouldSeeLicenseWithoutApplication() {
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
        lkSteps.onLkPage().link(USER_LICENSE_TEXT).should(isDisplayed());
        urlSteps.testing().path(LK_TENANT_QUESTIONNAIRE_EDIT).open();
        lkSteps.onLkPage().link(USER_LICENSE_TEXT).should(not(isDisplayed()));
        lkSteps.fillAnketa();
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.onLkPage().button("Предложения на Я.Недвижимости").should(isDisplayed());
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
        lkSteps.onLkPage().link(USER_LICENSE_TEXT).should(not(isDisplayed()));
        urlSteps.testing().path(LK_TENANT_QUESTIONNAIRE_EDIT).open();
        lkSteps.onLkPage().link(USER_LICENSE_TEXT).should(not(isDisplayed()));
    }
}
