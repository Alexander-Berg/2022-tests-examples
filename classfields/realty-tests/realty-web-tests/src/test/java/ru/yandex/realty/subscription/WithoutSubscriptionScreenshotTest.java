package ru.yandex.realty.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.SubscriptionSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.SUBSCRIPTIONS;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.page.SubscriptionPage.MY_SEARCHES_TAB;
import static ru.yandex.realty.page.SubscriptionPage.NEWBUILDING_TAB;
import static ru.yandex.realty.page.SubscriptionPage.PRICE_CHANGE_TAB;
import static ru.yandex.realty.step.UrlSteps.NEWBUILDING_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.PRICE_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.SEARCH_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;

@DisplayName("Подписки. Скриншот страницы без подписки")
@Link("https://st.yandex-team.ru/VERTISTEST-1475")
@Feature(SUBSCRIPTION)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class WithoutSubscriptionScreenshotTest {

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

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public String tabName;

    @Parameterized.Parameter(1)
    public String tabValue;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {MY_SEARCHES_TAB, SEARCH_TAB_VALUE},
                {NEWBUILDING_TAB, NEWBUILDING_TAB_VALUE},
                {PRICE_CHANGE_TAB, PRICE_TAB_VALUE}
        });
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Видим скриншот без подписки")
    public void shouldSeeWithoutSubscriptionsScreenshot() {
        api.createVos2Account(account, AccountType.OWNER);
        compareSteps.resize(1920, 3000);

        urlSteps.testing().path(SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, tabValue).open();
        subscriptionSteps.onSubscriptionPage().tab(tabName).waitUntil(hasClass(containsString("_tabSelected")));

        Screenshot testing = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage().subscriptionContent());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage()
                .subscriptionContent());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
