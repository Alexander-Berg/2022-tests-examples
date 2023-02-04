package ru.yandex.arenda.create;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.pages.LkPage.BIRTHDAY_ID;
import static ru.yandex.arenda.pages.LkPage.DEPARTMENT_CODE_ID;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_ISSUE_DATE_ID;
import static ru.yandex.arenda.pages.LkPage.PASSPORT_SERIES_AND_NUMBER_ID;
import static ru.yandex.arenda.pages.LkPage.SEND_TO_CHECK_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1722")
@DisplayName("Личные данные")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PersonalDataPassportValidatingTest {

    private static final String PASSPORT_TEXT = "9999999999";
    private static final String PASSPORT_ISSUE_BY = getRandomString();
    private static final String ISSUE_DATE_TEXT = "01012000";
    private static final String DEPARTMENT_TEXT = "123 - 456";
    private static final String BIRTHDAY_TEXT = "01011990";
    private static final String BIRTHPLACE_TEXT = getRandomString();
    private static final String NAME = "Валерий";
    private static final String SURNAME = "Валеридзе";
    private static final String PATRONYMIC = "Валерыч";

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

    @Parameterized.Parameter
    public String id;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> testParams() {
        return asList(PASSPORT_SERIES_AND_NUMBER_ID, PASSPORT_ISSUE_DATE_ID, DEPARTMENT_CODE_ID, BIRTHDAY_ID);
    }

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
        lkSteps.fillFormPersonalData(NAME, SURNAME, PATRONYMIC);
        lkSteps.fillPhonePersonalData();
        lkSteps.fillFormPassportData(PASSPORT_TEXT, PASSPORT_ISSUE_BY, ISSUE_DATE_TEXT, DEPARTMENT_TEXT, BIRTHDAY_TEXT,
                BIRTHPLACE_TEXT);
    }

    @Test
    @DisplayName("Не заполняем одно поле паспортных данных -> Видим ошибку")
    public void shouldSeeInvalidInput() {
        lkSteps.scrollElementToCenter(lkSteps.onLkPage().inputId(id).clearInputCross());
        lkSteps.onLkPage().inputId(id).clearInputCross().click();
        lkSteps.onLkPage().editPhoneButton().waitUntil(isDisplayed());
        lkSteps.onLkPage().button(SEND_TO_CHECK_BUTTON).click();
        lkSteps.onLkPage().invalidInputPersonalData(id).should(isDisplayed());
    }
}
