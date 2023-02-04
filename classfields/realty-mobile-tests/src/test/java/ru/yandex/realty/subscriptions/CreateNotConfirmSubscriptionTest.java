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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.mobile.element.subscriptions.SubscriptionPopup.ADJUST_SUBSCRIPTION;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.SEARCH_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;

@DisplayName("Подписки. Работа с неподтвержденными подписками")
@Link("https://st.yandex-team.ru/VERTISTEST-1475")
@Feature(SUBSCRIPTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class CreateNotConfirmSubscriptionTest {

    private String email;

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
        email = getRandomEmail();
        api.createNotConfirmSubscription(account.getId(), email);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, SEARCH_TAB_VALUE).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем подписку есть почта и и активная. Клик «отправить еще раз» -> «Подтверждение отправлено»")
    public void shouldSeeNotConfirmSubscription() {
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).should(hasText(containsString(email)));
        subscriptionSteps.onSubscriptionPage().subscription(FIRST).resendConfirmButton().should(isDisplayed()).click();
        subscriptionSteps.onSubscriptionPage().subscription(FIRST)
                .should(hasText(containsString("Подтверждение отправлено")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Удаляем неподтвержденную подписку")
    public void shouldDeleteSubscription() {
        subscriptionSteps.openDeletePopup();
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().button("Удалить").click();
        api.shouldSeeEmptySubscriptionList(account.getId());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем, что нет кнопки настройки у неподтвержденной подписки")
    public void shouldNotSeeAdjustButtonPopup() {
        subscriptionSteps.openDeletePopup();
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().button(ADJUST_SUBSCRIPTION).should(not(isDisplayed()));
    }
}
