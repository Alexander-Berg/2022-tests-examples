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
import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.pages.LkPage.NAME_ID;
import static ru.yandex.arenda.pages.LkPage.PATRONYMIC_ID;
import static ru.yandex.arenda.pages.LkPage.SEND_TO_CHECK_BUTTON;
import static ru.yandex.arenda.pages.LkPage.SURNAME_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1722")
@DisplayName("Личные данные")
@RunWith(Parameterized.class)
@GuiceModules(ArendaWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PersonalDataNamesValidatingTest {

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
    public String errorName;

    @Parameterized.Parameters(name = "для {0}")
    public static Collection<String> testParams() {
        return asList("Tovarish", "Петр5", "Петров - Водкин");
    }

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
    }

    @Test
    @DisplayName("Вводим ошибочное значение в поле фамилии -> видим ошибку")
    public void shouldSeeSurnameInputError() {
        lkSteps.onLkPage().inputId(SURNAME_ID).clearInputCross().clickIf(isDisplayed());
        lkSteps.onLkPage().inputId(SURNAME_ID).sendKeys(errorName);
        lkSteps.onLkPage().button(SEND_TO_CHECK_BUTTON).click();
        lkSteps.onLkPage().invalidInputPersonalData(SURNAME_ID).should(isDisplayed());
    }

    @Test
    @DisplayName("Вводим ошибочное значение в поле имени -> видим ошибку")
    public void shouldSeeNameInputError() {
        lkSteps.onLkPage().inputId(NAME_ID).clearInputCross().clickIf(isDisplayed());
        lkSteps.onLkPage().inputId(NAME_ID).sendKeys(errorName);
        lkSteps.onLkPage().button(SEND_TO_CHECK_BUTTON).click();
        lkSteps.onLkPage().invalidInputPersonalData(NAME_ID).should(isDisplayed());
    }

    @Test
    @DisplayName("Вводим ошибочное значение в поле имени -> видим ошибку")
    public void shouldSeePatronymicInputError() {
        lkSteps.onLkPage().inputId(PATRONYMIC_ID).clearInputCross().clickIf(isDisplayed());
        lkSteps.onLkPage().inputId(PATRONYMIC_ID).sendKeys(errorName);
        lkSteps.onLkPage().button(SEND_TO_CHECK_BUTTON).click();
        lkSteps.onLkPage().invalidInputPersonalData(PATRONYMIC_ID).should(isDisplayed());
    }
}
