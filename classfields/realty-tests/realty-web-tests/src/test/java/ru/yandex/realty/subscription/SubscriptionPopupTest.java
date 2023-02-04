package ru.yandex.realty.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.SubscriptionSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.awaitility.Awaitility.given;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier.SEARCH;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.page.SubscriptionPage.SAVE_BUTTON;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Подписки. Попап настройки")
@Link("https://st.yandex-team.ru/VERTISTEST-1475")
@Feature(SUBSCRIPTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SubscriptionPopupTest {

    private static final long DEFAULT_PERIOD = 60L;
    private static final String ONCE_A_DAY = "раз в день";

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
        api.createVos2Account(account, OWNER);
        api.createSubscription(account.getId(), SEARCH);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).open();
        subscriptionSteps.openSubscriptionPopup();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Меняем период и закрываем попап подписок -> период остается прежним")
    public void shouldNotSeeChangePeriod() {
        subscriptionSteps.selectItem(ONCE_A_DAY);
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().closeCross().click();
        api.shouldSeeLastSubscriptionEmailByUid(account.getId()).hasPeriod(DEFAULT_PERIOD);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Меняем email подписки")
    public void shouldChangeEmailAddress() {
        String newEmail = subscriptionSteps.changeEmail();
        shouldSeeSubscriptionWithEmail(account.getId(), newEmail);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @Description("Проверяем, что при пустом email он не сохраняется")
    public void shouldSeeDisabledButtonForEmptyEmail() {
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().clearEmailInput().click();
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().button(SAVE_BUTTON).should(isDisabled());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем, что крестик работает")
    public void shouldClosePopup() {
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().closeCross().click();
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().should(not(isDisplayed()));
    }

    @Step("Проверяем, что значение поля 'email' изменилось на {email}")
    public void shouldSeeSubscriptionWithEmail(String uid, String email) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).untilAsserted(()
                -> api.shouldSeeLastSubscriptionEmailByUid(uid).hasAddress(email));
    }
}
