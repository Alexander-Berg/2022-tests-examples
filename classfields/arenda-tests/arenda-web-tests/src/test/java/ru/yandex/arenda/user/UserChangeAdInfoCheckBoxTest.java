package ru.yandex.arenda.user;

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
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.matcher.AttributeMatcher.isChecked;
import static ru.yandex.arenda.pages.LkPage.SEND_TO_CHECK_BUTTON;

@Link("https://st.yandex-team.ru/VERTISTEST-2052")
@DisplayName("Галка на подписки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserChangeAdInfoCheckBoxTest {

    private static final String CHECKBOX_TEXT = "Согласен на получение";
    private static final String CHECKBOX_TEXT_AGREEMENT = "Я даю согласие";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        retrofitApiSteps.getUserId(uid);
        passportSteps.login(account);
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
    }

    @Test
    @DisplayName("берем юзера с проставленной галкой, отключаем его смотрим настройки ее нет")
    public void shouldSeeSavedTickOnAdInfo() {
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(not(isChecked())).click();
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(isChecked());
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT_AGREEMENT).click();
        lkSteps.onLkPage().button(SEND_TO_CHECK_BUTTON).click();
        lkSteps.onLkPage().successToast();
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(isChecked()).click();
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(not(isChecked()));
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT_AGREEMENT).click();
        lkSteps.onLkPage().button(SEND_TO_CHECK_BUTTON).click();
        lkSteps.onLkPage().successToast();
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(not(isChecked()));

    }
}
