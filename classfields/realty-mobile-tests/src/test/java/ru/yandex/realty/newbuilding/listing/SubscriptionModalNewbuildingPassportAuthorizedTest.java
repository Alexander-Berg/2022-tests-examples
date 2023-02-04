package ru.yandex.realty.newbuilding.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.RetrofitApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static ru.auto.test.api.realty.SubscriptionStatus.ACTIVE;
import static ru.auto.test.api.realty.SubscriptionStatus.AWAIT_CONFIRMATION;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isEnabled;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.SUBSCRIPTIONS;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal.CONFIRMATION;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal.PODPISKI;
import static ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal.SUBSCRIBE;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.NEWBUILDING_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@Issue("VERTISTEST-1352")
@Epic(NEWBUILDING)
@Feature(SUBSCRIPTION)
@DisplayName("Модалка подписки авторизованного в паспорте")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SubscriptionModalNewbuildingPassportAuthorizedTest {

    private String email;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;


    @Before
    public void before() {
        api.createYandexAccount(account);
        email = getRandomEmail();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).open();
        basePageSteps.onNewBuildingPage().site(FIRST).favorite().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Email соответствует")
    public void shouldSeeEmailFromPassport() {
        basePageSteps.onNewBuildingPage().subscriptionModal().emailInput().should(hasValue(
                String.format("%s@yandex.ru", account.getLogin())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Кнопка «Подписаться» активна")
    public void shouldSeeSubscribeButtonEnabledPassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).should(isEnabled());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Чекбокс подписки на спам активен")
    public void shouldSeeCheckboxPassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().checkbox().should(isChecked());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Ссылка на «Подписки» после оформления")
    public void shouldSeeUrlToSubscriptionsPassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();
        basePageSteps.onNewBuildingPage().subscriptionModal().subscriptionSuccess().waitUntil(isDisplayed());

        basePageSteps.onNewBuildingPage().subscriptionModal().link(PODPISKI).should(hasHref(equalTo(
                urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, NEWBUILDING_TAB_VALUE).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Подписка активирована")
    public void shouldSeeSubscriptionActivePassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();

        api.checkSubscriptionInStatus(account.getId(), ACTIVE);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Подписка активирована, без промо")
    public void shouldSeeSubscriptionActiveWithoutPromoPassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().checkbox().click();
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();

        api.checkSubscriptionInStatus(account.getId(), ACTIVE);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Промо подписка подтверждена")
    public void shouldSeeSubscriptionToPromoActivePassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();

        retrofitApiSteps.checkPromoSubscriptionAgreed(account.getId(), true);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Промо подписка не активна")
    public void shouldSeeSubscriptionToPromoNotFoundPassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().checkbox().click();
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();

        retrofitApiSteps.checkPromoSubscriptionNotFound(account.getId());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. После рефреша подписка активна")
    public void shouldSeeSuccessSubscriptionAfterRefreshPassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();
        basePageSteps.refresh();
        basePageSteps.onNewBuildingPage().site(FIRST).favorite().should(hasClass(containsString("_active")));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Нет чекбокса на промо, при активной подписке на промо")
    public void shouldNotSeePromoCheckboxWithActivePromoPassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();
        retrofitApiSteps.checkPromoSubscriptionAgreed(account.getId(), true);
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).open();
        basePageSteps.onNewBuildingPage().site(FIRST).favorite().click();

        basePageSteps.onNewBuildingPage().subscriptionModal().checkbox().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка подписки. Юзер из паспорта. Подтверждение емейла отличного от паспортного")
    public void shouldSeeEmailConfirmationPassportAuthorized() {
        basePageSteps.onNewBuildingPage().subscriptionModal().clearInput().click();
        basePageSteps.onNewBuildingPage().subscriptionModal().emailInput().sendKeys(email);
        basePageSteps.onNewBuildingPage().subscriptionModal().button(SUBSCRIBE).click();
        basePageSteps.onNewBuildingPage().subscriptionModal().subscriptionSuccess().waitUntil(isDisplayed());

        basePageSteps.onNewBuildingPage().subscriptionModal().description().should(hasText(
                String.format("%s %s.", CONFIRMATION, email)));
        api.checkSubscriptionInStatus(account.getId(), AWAIT_CONFIRMATION);
    }

}
