package ru.yandex.realty.subscriptions;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.SubscriptionSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.awaitility.Awaitility.given;
import static ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier.SEARCH;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.mobile.element.subscriptions.SubscriptionPopup.IMMEDIATELY;
import static ru.yandex.realty.mobile.element.subscriptions.SubscriptionPopup.ONCE_A_DAY;
import static ru.yandex.realty.mobile.element.subscriptions.SubscriptionPopup.ONCE_A_WEEK;
import static ru.yandex.realty.mobile.element.subscriptions.SubscriptionPopup.SAVE_BUTTON;
import static ru.yandex.realty.step.UrlSteps.SEARCH_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Подписки. Попап настройки. Период")
@Link("https://st.yandex-team.ru/VERTISTEST-1475")
@Feature(SUBSCRIPTION)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SubscriptionPopupPeriodTest {

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

    @Parameterized.Parameter
    public String periodItem;

    @Parameterized.Parameter(1)
    public Long value;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {IMMEDIATELY, 1L},
                {ONCE_A_DAY, 1440L},
                {ONCE_A_WEEK, 10080L},
        });
    }

    @Before
    public void before() {
        api.createVos2Account(account, OWNER);
        api.createSubscription(account.getId(), SEARCH);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, SEARCH_TAB_VALUE).open();
        subscriptionSteps.openSubscriptionPopup();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем период получения. Период сохраняется")
    public void shouldChangePeriod() {
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().button(periodItem).click();
        subscriptionSteps.onSubscriptionPage().subscriptionPopup().button(SAVE_BUTTON).click();
        shouldSeeSubscriptionWithPeriod(account.getId(), value);
    }

    @Step("Проверяем, что начение поля «period» изменилось на {period}")
    public void shouldSeeSubscriptionWithPeriod(String uid, long period) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).untilAsserted(()
                -> api.shouldSeeLastSubscriptionEmailByUid(uid).hasPeriod(period));
    }
}
