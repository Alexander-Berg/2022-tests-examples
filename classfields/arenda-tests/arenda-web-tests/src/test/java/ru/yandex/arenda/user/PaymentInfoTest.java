package ru.yandex.arenda.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.constants.UriPath;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.ApiSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.arenda.constants.UriPath.EDIT;
import static ru.yandex.arenda.constants.UriPath.LK;
import static ru.yandex.arenda.pages.BasePage.CARDS_FOR_PAYMENT;
import static ru.yandex.arenda.pages.BasePage.DATA_ACCOUNT;
import static ru.yandex.arenda.pages.LkPage.ACCOUNT_NUMBER_ID;
import static ru.yandex.arenda.pages.LkPage.BIK_ID;
import static ru.yandex.arenda.pages.LkPage.INN_ID;
import static ru.yandex.arenda.pages.LkPage.SAVE_BUTTON;
import static ru.yandex.arenda.utils.UtilsWeb.getObjectFromJson;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1725")
@DisplayName("[Arenda] Тесты на страницу «Платежная информация»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class PaymentInfoTest {

    private static final String PAYMENT_DATA = "paymentData";
    private static final String PATCH_USER_PATH = "realty3api/patch_user.json";
    private static final String EDIT_BUTTON = "Редактировать";
    private static final String TEST_INN = "132808730606";
    private static final String TEST_ACCOUNT_NIMBER = "12345678901234567890";
    private static final String TEST_BIK = "044525225";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Before
    public void before() {
        apiSteps.createYandexAccount(account);
    }

    @Test
    @DisplayName("Если нет заявок -> в меню нет «Данные счёта», «Карта для выплаты»")
    public void shouldNotSeePaymentInfo() {
        urlSteps.testing().path(LK).open();
        lkSteps.onLkPage().myCabinet().click();
        lkSteps.onLkPage().myCabinetPopupDesktop().should(isDisplayed());
        lkSteps.onLkPage().myCabinetPopupDesktop().link(DATA_ACCOUNT).should(not(isDisplayed()));
        lkSteps.onLkPage().myCabinetPopupDesktop().link(CARDS_FOR_PAYMENT).should(not(isDisplayed()));
    }

    @Test
    @DisplayName("Если есть заявки -> в меню есть «Данные счёта», «Карта для выплаты»")
    public void shouldSeePaymentInfoRaw() {
        String uid = account.getId();
        retrofitApiSteps.getUserByUid(uid);
        JsonObject patch = getObjectFromJson(JsonObject.class, PATCH_USER_PATH);
        patch.remove(PAYMENT_DATA);
        retrofitApiSteps.patchUserByUid(uid, patch);
        retrofitApiSteps.createConfirmedFlat(uid);

        urlSteps.testing().path(LK).open();
        lkSteps.onLkPage().myCabinet().click();
        lkSteps.onLkPage().myCabinetPopupDesktop().should(isDisplayed());
        lkSteps.onLkPage().myCabinetPopupDesktop().link(DATA_ACCOUNT).should(isDisplayed());
        lkSteps.onLkPage().myCabinetPopupDesktop().link(CARDS_FOR_PAYMENT).should(isDisplayed());
    }

    @Test
    @DisplayName("Первичное заполнение платежных данных -> Редирект на презентационную, на редактирование уже открыть нельзя")
    public void shouldFillPaymentInfo() {
        String uid = account.getId();
        retrofitApiSteps.getUserByUid(uid);
        JsonObject patch = getObjectFromJson(JsonObject.class, PATCH_USER_PATH);
        patch.remove(PAYMENT_DATA);
        retrofitApiSteps.patchUserByUid(uid, patch);
        retrofitApiSteps.createConfirmedFlat(uid);

        urlSteps.testing().path(LK).path(UriPath.PAYMENT_DATA).open();
        lkSteps.onLkPage().link(EDIT_BUTTON).click();

        urlSteps.testing().path(LK).path(UriPath.PAYMENT_DATA).path(EDIT).shouldNotDiffWithWebDriverUrl();
        lkSteps.onLkPage().inputId(INN_ID).sendKeys(TEST_INN);
        lkSteps.onLkPage().inputId(ACCOUNT_NUMBER_ID).sendKeys(TEST_ACCOUNT_NIMBER);
        lkSteps.onLkPage().inputId(BIK_ID).sendKeys(TEST_BIK);
        lkSteps.onLkPage().button(SAVE_BUTTON).click();
        lkSteps.onLkPage().confirmModal().button(SAVE_BUTTON).click();
        urlSteps.testing().path(LK).path(UriPath.PAYMENT_DATA).shouldNotDiffWithWebDriverUrl();
        lkSteps.onLkPage().link(EDIT_BUTTON).should(not(isDisplayed()));
    }
}
