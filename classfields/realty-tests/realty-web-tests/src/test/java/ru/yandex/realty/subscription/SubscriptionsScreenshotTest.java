package ru.yandex.realty.subscription;

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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.SubscriptionSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier.NEWBUILDING;
import static ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier.PRICE;
import static ru.yandex.realty.adaptor.BackRtAdaptor.SubscriptionQualifier.SEARCH;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.SUBSCRIPTION;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.NEWBUILDING_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.PRICE_TAB_VALUE;
import static ru.yandex.realty.step.UrlSteps.TAB_URL_PARAM;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Подписки. Скриншоты")
@Link("https://st.yandex-team.ru/VERTISTEST-1475")
@Feature(SUBSCRIPTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class SubscriptionsScreenshotTest {

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

    @Before
    public void before() {
        api.createVos2Account(account, OWNER);
        compareSteps.resize(1920, 3000);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот попапа")
    public void shouldSeePopupScreenshot() {
        api.createSubscription(account.getId(), SEARCH);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).open();
        subscriptionSteps.openSubscriptionPopup();
        Screenshot testing = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage().subscriptionPopup());

        urlSteps.setProductionHost().open();
        subscriptionSteps.openSubscriptionPopup();
        Screenshot production = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage().subscriptionPopup());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот неподтвержденной подписки")
    public void shouldSeeNotConfirmedScreenshot() {
        api.createNotConfirmSubscription(account.getId(), getRandomEmail());
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).open();
        Screenshot testing = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage().subscriptionContent());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage()
                .subscriptionContent());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот выключенной подписки")
    public void shouldSeeDisabledScreenshot() {
        api.createDisabledSubscription(account.getId(), PRICE);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, PRICE_TAB_VALUE).open();
        Screenshot testing = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage().subscriptionContent());

        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage()
                .subscriptionContent());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот попапа удаления")
    public void shouldSeeDeletePopupScreenshot() {
        api.createDisabledSubscription(account.getId(), NEWBUILDING);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, NEWBUILDING_TAB_VALUE).open();
        subscriptionSteps.openDeletePopup();
        Screenshot testing = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage().deletePopup());

        urlSteps.setProductionHost().open();
        subscriptionSteps.openDeletePopup();
        Screenshot production = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage().deletePopup());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот удаленной подписки")
    public void shouldSeeDeletedScreenshot() {
        String body = getRandomString();
        String title = getRandomString();
        String email = getRandomEmail();
        api.createSubscriptionWithReq(account.getId(), NEWBUILDING, body, title, email);
        urlSteps.testing().path(Pages.SUBSCRIPTIONS).queryParam(TAB_URL_PARAM, NEWBUILDING_TAB_VALUE).open();
        subscriptionSteps.openDeletePopup();
        subscriptionSteps.onSubscriptionPage().deletePopup().button("Удалить").click();
        Screenshot testing = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage().subscription(FIRST));

        api.createSubscriptionWithReq(account.getId(), NEWBUILDING, body, title, email);
        urlSteps.setProductionHost().open();
        subscriptionSteps.openDeletePopup();
        subscriptionSteps.onSubscriptionPage().deletePopup().button("Удалить").click();
        Screenshot production = compareSteps.takeScreenshot(subscriptionSteps.onSubscriptionPage().subscription(FIRST));
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
