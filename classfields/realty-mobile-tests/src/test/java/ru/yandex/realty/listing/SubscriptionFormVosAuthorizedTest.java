package ru.yandex.realty.listing;

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

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.test.api.realty.SubscriptionStatus.AWAIT_CONFIRMATION;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isEnabled;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.SUBSCRIPTIONS;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mobile.element.listing.SubscriptionForm.CONFIRMATION;
import static ru.yandex.realty.mobile.element.listing.SubscriptionForm.PODPISKI;
import static ru.yandex.realty.mobile.element.listing.SubscriptionForm.SUBSCRIBE;
import static ru.yandex.realty.step.UrlSteps.SEARCH_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.getRandomUserRequestBody;

@Issue("VERTISTEST-1352")
@Epic(LISTING)
@Feature(SUBSCRIPTION)
@DisplayName("Блок подписки авторизованного в VOS'e")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SubscriptionFormVosAuthorizedTest {

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
        email = getRandomEmail();
        api.createVos2Account(account, getRandomUserRequestBody(account.getId(), OWNER.getValue()).withEmail(email));
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. Email соответствует")
    public void shouldSeeEmailFromVOS() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().emailInput().should(hasValue(email));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. Кнопка «Подписаться» активна")
    public void shouldSeeSubscribeButtonEnabledVOSAuthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE)
                .should(isEnabled());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. Чекбокс подписки на спам активен")
    public void shouldSeeCheckboxVOSAuthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().checkbox().should(isChecked());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. Текст подтверждения емейла")
    public void shouldSeeEmailConfirmationUnauthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();

        basePageSteps.onMobileSaleAdsPage().subscriptionForm().description().should(hasText(
                String.format("%s %s.", CONFIRMATION, email)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. Ссылка на «Подписки» после оформления")
    public void shouldSeeUrlToSubscriptionsVOSAuthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();

        basePageSteps.onMobileSaleAdsPage().subscriptionForm().link(PODPISKI).should(hasHref(equalTo(
                urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, SEARCH_TAB_VALUE).toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. Подписка ожидает подтверждения")
    public void shouldSeeSubscriptionActiveVOSAuthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();

        api.checkSubscriptionInStatus(account.getId(), AWAIT_CONFIRMATION);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. Подписка ожидает подтверждения, без промо")
    public void shouldSeeSubscriptionActiveWithoutPromoVOSAuthorized() {
        basePageSteps.scrollToElement(basePageSteps.onMobileSaleAdsPage().subscriptionForm());
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().checkbox().click();
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();

        api.checkSubscriptionInStatus(account.getId(), AWAIT_CONFIRMATION);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. Промо подписка не подтверждена")
    public void shouldSeeSubscriptionToPromoActiveVOSAuthorized() {
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();

        retrofitApiSteps.checkPromoSubscriptionAgreed(account.getId(), false);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. Промо подписка не подключена")
    public void shouldSeeSubscriptionToPromoNotFoundVOSAuthorized() {
        basePageSteps.scrollToElement(basePageSteps.onMobileSaleAdsPage().subscriptionForm());
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().checkbox().click();
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();

        retrofitApiSteps.checkPromoSubscriptionNotFound(account.getId());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок подписки. Юзер из VOS'а. После рефреша - подтверждение емейла")
    public void shouldSeeSuccessSubscriptionAfterRefreshVOSAuthorized() {
        basePageSteps.scrollToElement(basePageSteps.onMobileSaleAdsPage().subscriptionForm());
        basePageSteps.onMobileSaleAdsPage().subscriptionForm().button(SUBSCRIBE).click();
        basePageSteps.refresh();

        basePageSteps.onMobileSaleAdsPage().subscriptionForm().description().should(hasText(
                String.format("%s %s.", CONFIRMATION, email)));
    }

}
