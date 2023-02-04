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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.EDIT;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.HOUSE_SERVICES;
import static ru.yandex.arenda.constants.UriPath.LK;
import static ru.yandex.arenda.constants.UriPath.LK_SDAM;
import static ru.yandex.arenda.constants.UriPath.OWNER;
import static ru.yandex.arenda.constants.UriPath.SETTINGS;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.BY_MYSELF_LABEL;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.CANCEL_BUTTON;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.NO_LABEL;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.PAID_BY_TENANT_AMOUNT_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.PAID_BY_TENANT_HOUSE_SERVICES_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.PAYMENT_AMOUNT_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.PAYMENT_DETAILS_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.SAVE_SETTINGS;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.SEND_BUTTON;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.SEND_TO_TENANT_BUTTON;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.TENANT_LABEL;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.TENANT_REFUND_PAYMENTS_DESCRIPTION_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.TENANT_REFUND_PAYMENT_AMOUNT_ID;
import static ru.yandex.arenda.pages.LkOwnerHouseServiceAdjustPage.YES_LABEL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-2040")
@DisplayName("ЖКХ. Флоу собственника")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserHouseServiceTest {

    private static final String SENT_TEXT = "Ожидаем подтверждения условий по коммуналке от жильца";
    private static final String PRE_SEND_TEXT =
            "Если вы заполнили все настройки по коммуналке, то отправьте их на подтверждение жильцу.";

    String createdFlatId;

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
        createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        String ownerRequestId = retrofitApiSteps.getOwnerRequestID(createdFlatId);
        retrofitApiSteps.updateStatusInProgress(createdFlatId, ownerRequestId);
        passportSteps.login(account);
    }

    @Test
    @DisplayName("Кнопка «Настроить» ведет в настройки")
    public void shouldSeeAdjustButton() {
        urlSteps.testing().path(LK_SDAM).open();
        lkSteps.onLkOwnerFlatListingPage().adjustJkhButton().click();
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).ignoreParam("spaFromPage").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Кто будет заниматься оплатой коммуналки? -> Я сам -> Хотите, чтобы жилец возмещал -> Да -> " +
            "видим поля. .. -> Нет -> поля пропадают")
    public void shouldSeeTenantRefundFields() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(BY_MYSELF_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().textAreaId(TENANT_REFUND_PAYMENTS_DESCRIPTION_ID)
                .should(not(isDisplayed()));
        lkSteps.onLkOwnerHouseServiceAdjustPage().inputId(TENANT_REFUND_PAYMENT_AMOUNT_ID).should(not(isDisplayed()));
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantRefundBlock().label(YES_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().textAreaId(TENANT_REFUND_PAYMENTS_DESCRIPTION_ID)
                .should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceAdjustPage().inputId(TENANT_REFUND_PAYMENT_AMOUNT_ID).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantRefundBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().textAreaId(TENANT_REFUND_PAYMENTS_DESCRIPTION_ID)
                .should(not(isDisplayed()));
        lkSteps.onLkOwnerHouseServiceAdjustPage().inputId(TENANT_REFUND_PAYMENT_AMOUNT_ID).should(not(isDisplayed()));
    }

    @Test
    @DisplayName("Есть услуги, которые жилец должен оплачивать самостоятельно? -> Я сам -> " +
            "Хотите, чтобы жилец возмещал -> Да -> видим поля. .. -> Нет -> поля пропадают")
    public void shouldSeePaidByTenantFields() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(BY_MYSELF_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().textAreaId(PAID_BY_TENANT_HOUSE_SERVICES_ID)
                .should(not(isDisplayed()));
        lkSteps.onLkOwnerHouseServiceAdjustPage().inputId(PAID_BY_TENANT_AMOUNT_ID).should(not(isDisplayed()));
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantPaidByTenantBlock().label(YES_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().textAreaId(PAID_BY_TENANT_HOUSE_SERVICES_ID).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceAdjustPage().inputId(PAID_BY_TENANT_AMOUNT_ID).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantPaidByTenantBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().textAreaId(PAID_BY_TENANT_HOUSE_SERVICES_ID)
                .should(not(isDisplayed()));
        lkSteps.onLkOwnerHouseServiceAdjustPage().inputId(PAID_BY_TENANT_AMOUNT_ID).should(not(isDisplayed()));
    }

    @Test
    @DisplayName("Есть услуги, которые жилец должен оплачивать самостоятельно? -> Я сам -> " +
            "Хотите, чтобы жилец возмещал вам деньги -> пытаемся сохранить пустые -> поля видим ошибки")
    public void shouldSeeTrySaveEmptyFormPaidTenantBlock() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(BY_MYSELF_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantPaidByTenantBlock().label(YES_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SAVE_SETTINGS).click();

        lkSteps.onLkOwnerHouseServiceAdjustPage().errorDescription("Обязательное поле").should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceAdjustPage().errorDescription("Некорректное значение для суммы")
                .should(isDisplayed());
    }

    @Test
    @DisplayName("Есть услуги, которые жилец должен оплачивать самостоятельно? -> Я сам -> " +
            "Хотите, чтобы жилец возмещал вам деньги -> пытаемся сохранить пустые -> поля видим ошибки")
    public void shouldSeeTrySaveEmptyFormTenantRefundBlock() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(BY_MYSELF_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantRefundBlock().label(YES_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SAVE_SETTINGS).click();

        lkSteps.onLkOwnerHouseServiceAdjustPage().errorDescription("Обязательное поле").should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceAdjustPage().errorDescription("Некорректное значение для суммы")
                .should(isDisplayed());
    }

    @Test
    @DisplayName("Есть услуги, которые жилец должен оплачивать самостоятельно? -> Я сам -> " +
            "сохраняем без значений")
    public void shouldSeeSavedSettingsWithoutValues() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(BY_MYSELF_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantRefundBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantPaidByTenantBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SAVE_SETTINGS).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().successToast();
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES).path(SETTINGS)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Есть услуги, которые жилец должен оплачивать самостоятельно? -> Я сам -> " +
            "сохраняем со значениями")
    public void shouldSeeSavedSettingsWithValues() {
        String testText = getRandomString();
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(BY_MYSELF_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantPaidByTenantBlock().label(YES_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().textAreaId(PAID_BY_TENANT_HOUSE_SERVICES_ID).sendKeys(testText);
        lkSteps.onLkOwnerHouseServiceAdjustPage().inputId(PAID_BY_TENANT_AMOUNT_ID).sendKeys(testText);

        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantRefundBlock().label(YES_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().textAreaId(TENANT_REFUND_PAYMENTS_DESCRIPTION_ID).sendKeys(testText);
        lkSteps.onLkOwnerHouseServiceAdjustPage().inputId(TENANT_REFUND_PAYMENT_AMOUNT_ID).sendKeys(testText);

        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SAVE_SETTINGS).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().successToast();
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES).path(SETTINGS)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @DisplayName("Есть услуги, которые жилец должен оплачивать самостоятельно? -> Я сам -> " +
            "отправить жильцу? -> отмена")
    public void shouldSeeCanceledSendToTenant() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(BY_MYSELF_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantRefundBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantPaidByTenantBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SAVE_SETTINGS).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().successToast();
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES).path(SETTINGS)
                .shouldNotDiffWithWebDriverUrl();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tempMessage().should(hasText(PRE_SEND_TEXT));
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SEND_TO_TENANT_BUTTON).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().sendTenantPopup().button(CANCEL_BUTTON).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().sendTenantPopup().should(not(isDisplayed()));
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SEND_TO_TENANT_BUTTON).should(isDisplayed());
        lkSteps.onLkOwnerHouseServiceAdjustPage().tempMessage().should(hasText(PRE_SEND_TEXT));
        urlSteps.shouldNotDiffWithWebDriverUrl();

    }

    @Test
    @DisplayName("Есть услуги, которые жилец должен оплачивать самостоятельно? -> Я сам -> " +
            "отправить жильцу? -> отмена")
    public void shouldSeeSendToTenant() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(BY_MYSELF_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantRefundBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tenantPaidByTenantBlock().label(NO_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SAVE_SETTINGS).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().successToast();
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES).path(SETTINGS)
                .shouldNotDiffWithWebDriverUrl();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tempMessage().should(hasText(PRE_SEND_TEXT));
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SEND_TO_TENANT_BUTTON).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().sendTenantPopup().button(SEND_BUTTON).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tempMessage().should(hasText(containsString(SENT_TEXT)));
    }

    @Test
    @DisplayName("Кто будет заниматься оплатой коммуналки? -> Жилец -> отправляем жильцу")
    public void shouldSeePayTenantLabel() {
        urlSteps.testing().path(LK).path(OWNER).path(FLAT).path(createdFlatId).path(HOUSE_SERVICES)
                .path(SETTINGS).path(EDIT).open();
        lkSteps.onLkOwnerHouseServiceAdjustPage().labelThatContains(TENANT_LABEL).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().textAreaId(PAYMENT_DETAILS_ID).sendKeys(getRandomString());
        lkSteps.onLkOwnerHouseServiceAdjustPage().inputId(PAYMENT_AMOUNT_ID).sendKeys(getRandomString());
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SAVE_SETTINGS).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().successToast();
        lkSteps.onLkOwnerHouseServiceAdjustPage().button(SEND_TO_TENANT_BUTTON).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().sendTenantPopup().button(SEND_BUTTON).click();
        lkSteps.onLkOwnerHouseServiceAdjustPage().tempMessage().should(hasText(containsString(SENT_TEXT)));
    }
}
