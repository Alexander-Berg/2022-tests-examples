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
import static ru.yandex.arenda.constants.UriPath.LK_FLAT;
import static ru.yandex.arenda.constants.UriPath.LK_PERSONAL_DATA_EDIT;
import static ru.yandex.arenda.constants.UriPath.LK_SDAT_KVARTIRY;
import static ru.yandex.arenda.matcher.AttributeMatcher.isChecked;
import static ru.yandex.arenda.pages.LkPage.ADDRESS_ID;
import static ru.yandex.arenda.pages.LkPage.CONFIRMATION_CODE_ID;
import static ru.yandex.arenda.pages.LkPage.CONFIRMATION_CODE_NUMBER;
import static ru.yandex.arenda.pages.LkPage.CONFIRM_BUTTON;
import static ru.yandex.arenda.pages.LkPage.FLAT_NUMBER_ID;
import static ru.yandex.arenda.pages.LkPage.PASS_TO_LK_BUTTON;
import static ru.yandex.arenda.pages.LkPage.SEND_BUTTON;
import static ru.yandex.arenda.pages.LkPage.TEST_ADDRESS;
import static ru.yandex.arenda.pages.LkPage.TEST_FLAT_NUMBER;

@Link("https://st.yandex-team.ru/VERTISTEST-2052")
@DisplayName("Галка на подписки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserOwnerAdInfoCheckBoxTest {

    private static final String CHECKBOX_TEXT = "Согласен на получение";
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
    }

    @Test
    @DisplayName("Оставляем галку сохраняем нового юзера смотрим в инфе о юзере есть галка")
    public void shouldSeeSavedTickOnAdInfo() {
        urlSteps.testing().path(LK_SDAT_KVARTIRY).open();
        lkSteps.onLkPage().inputId(ADDRESS_ID).sendKeys(TEST_ADDRESS);
        lkSteps.onLkPage().suggestItem(TEST_ADDRESS).click();
        lkSteps.onLkPage().inputId(FLAT_NUMBER_ID).sendKeys(TEST_FLAT_NUMBER);
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(isChecked());
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.onLkPage().inputId(CONFIRMATION_CODE_ID).sendKeys(CONFIRMATION_CODE_NUMBER);
        lkSteps.onLkPage().button(CONFIRM_BUTTON).click();
        lkSteps.onLkPage().successModal().button(PASS_TO_LK_BUTTON).click();
        urlSteps.waitForUrl(LK_FLAT, 20);
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(isChecked());
    }

    @Test
    @DisplayName("Убираем галку сохраняем нового юзера смотрим в инфе о юзере нет галки")
    public void shouldNotSeeSavedTickOnAdInfo() {
        urlSteps.testing().path(LK_SDAT_KVARTIRY).open();
        System.out.println();
        lkSteps.onLkPage().inputId(ADDRESS_ID).sendKeys(TEST_ADDRESS);
        lkSteps.onLkPage().suggestItem(TEST_ADDRESS).click();
        lkSteps.onLkPage().inputId(FLAT_NUMBER_ID).sendKeys(TEST_FLAT_NUMBER);
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(isChecked()).click();
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(not(isChecked()));
        lkSteps.onLkPage().button(SEND_BUTTON).click();
        lkSteps.onLkPage().inputId(CONFIRMATION_CODE_ID).sendKeys(CONFIRMATION_CODE_NUMBER);
        lkSteps.onLkPage().button(CONFIRM_BUTTON).click();
        lkSteps.onLkPage().successModal().button(PASS_TO_LK_BUTTON).click();
        urlSteps.waitForUrl(LK_FLAT, 20);
        urlSteps.testing().path(LK_PERSONAL_DATA_EDIT).open();
        lkSteps.onLkPage().labelThatContains(CHECKBOX_TEXT).waitUntil(not(isChecked()));
    }
}
