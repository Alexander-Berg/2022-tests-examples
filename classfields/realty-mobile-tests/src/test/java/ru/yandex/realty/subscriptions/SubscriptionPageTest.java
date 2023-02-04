package ru.yandex.realty.subscriptions;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.SubscriptionSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier.NEWBUILDING;
import static ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier.PRICE;
import static ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier.SEARCH;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;
import static ru.yandex.realty.mobile.page.SubscriptionPage.MY_SEARCHES_TAB;
import static ru.yandex.realty.mobile.page.SubscriptionPage.NEWBUILDING_TAB;
import static ru.yandex.realty.mobile.page.SubscriptionPage.PRICE_CHANGE_TAB;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.NEWBUILDING_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.PRICE_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.SEARCH_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;

@DisplayName("Подписки. Страница подписок")
@Link("https://st.yandex-team.ru/VERTISTEST-1475")
@Feature(SUBSCRIPTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SubscriptionPageTest {

    private static final String DELETE = "Удалить";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ApiSteps api;

    @Inject
    private Account account;

    @Inject
    private SubscriptionSteps subscriptionSteps;

    @Before
    public void before() {
        api.createYandexAccount(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Удаляем подписку")
    public void shouldDeleteSubscription() {
        api.createSubscription(account.getId(), SEARCH);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, SEARCH_TAB_VALUE).open();
        subscriptionSteps.openDeletePopup();
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().button(DELETE).click();
        api.shouldSeeEmptySubscriptionList(account.getId());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем попап -> Не удаляем подписку")
    public void shouldNotDeleteSubscription() {
        api.createSubscription(account.getId(), SEARCH);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, SEARCH_TAB_VALUE).open();
        subscriptionSteps.openDeletePopup();
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().closeCross().click();
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().should(not(isDisplayed()));
        api.shouldSeeLastSubscriptionByUid(account.getId()).isNotNull();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Тумблер подписки включает уведомление")
    public void shouldSeeEnabledSubscription() {
        api.createDisabledSubscription(account.getId(), PRICE);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, PRICE_TAB_VALUE).open();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).checkTumbler();
        api.shouldSeeLastSubscriptionByUid(account.getId()).hasDisabled(false);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Тумблер подписки выключает уведомление")
    public void shouldSeeDisabledSubscription() {
        api.createSubscription(account.getId(), NEWBUILDING);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, NEWBUILDING_TAB_VALUE).open();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).unCheckTumbler();
        api.shouldSeeLastSubscriptionByUid(account.getId()).hasDisabled(true);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Тумблер «все уведомления» отключает все уведомления во всех разделах")
    public void shouldSeeDisabledAllSubscription() {
        api.createSubscription(account.getId(), SEARCH);
        api.createSubscription(account.getId(), NEWBUILDING);
        api.createSubscription(account.getId(), PRICE);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, NEWBUILDING_TAB_VALUE).open();
        subscriptionSteps.onSubscriptionPage().unCheckAllSubscriptions();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).tumbler().should(not(isChecked()));
        subscriptionSteps.onSubscriptionPage().navButtonBack().click();
        subscriptionSteps.onSubscriptionPage().tab(NEWBUILDING_TAB).click();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).tumbler().should(not(isChecked()));
        subscriptionSteps.onSubscriptionPage().navButtonBack().click();
        subscriptionSteps.onSubscriptionPage().tab(PRICE_CHANGE_TAB).click();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).tumbler().should(not(isChecked()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Тумблер «все уведомления» включает все уведомления во всех разделах")
    public void shouldSeeEnableAllSubscription() {
        api.createDisabledSubscription(account.getId(), SEARCH);
        api.createDisabledSubscription(account.getId(), NEWBUILDING);
        api.createDisabledSubscription(account.getId(), PRICE);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, PRICE_TAB_VALUE).open();
        subscriptionSteps.onSubscriptionPage().checkAllSubscriptions();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).tumbler().should(isChecked());
        subscriptionSteps.onSubscriptionPage().navButtonBack().click();
        subscriptionSteps.onSubscriptionPage().tab(NEWBUILDING_TAB).click();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).tumbler().should(isChecked());
        subscriptionSteps.onSubscriptionPage().navButtonBack().click();
        subscriptionSteps.onSubscriptionPage().tab(MY_SEARCHES_TAB).click();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).tumbler().should(isChecked());
    }
}
