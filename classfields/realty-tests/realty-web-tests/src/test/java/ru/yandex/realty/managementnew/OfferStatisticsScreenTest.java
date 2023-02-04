package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.time.LocalDateTime.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;
import static ru.yandex.realty.utils.RealtyUtils.reformatOfferCreateDate;

/**
 * @author kurau (Yuri Kalinin)
 */
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class OfferStatisticsScreenTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private Account account1;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void openManagementPage() {
        apiSteps.createVos2AccountWithoutLogin(account, OWNER);
        offerBuildingSteps.addNewOffer(account).withSearcherWait()
                .withBody(getDefaultOffer(APARTMENT_SELL)
                        .withCreateTime(reformatOfferCreateDate(now().minusDays(1)))
                        .withUpdateTime(reformatOfferCreateDate(now().minusDays(1)))).create();
        passportSteps.login(account1);
        urlSteps.testing().path(Pages.OFFER).path(offerBuildingSteps.getId()).open();
        basePageSteps.onOfferCardPage().authorBlock().waitUntil(isDisplayed());
        passportSteps.logoff();
        passportSteps.login(account);
        compareSteps.resize(1920, 3000);
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        refreshUntilSee();
        managementSteps.openOfferStatistic(FIRST);
    }

    @Ignore("Надо бы замокать")
    @Test
    @Owner(KURAU)
    @Category({Regression.class, Screenshot.class, Testing.class})
    @DisplayName("Скрин: Оффер со статистикой")
    public void screenOfferWithStatistics() {
        Screenshot testingScreenshot = compareSteps.expScreen(
                managementSteps.onManagementNewPage().offer(FIRST).should(isDisplayed()));

        compareSteps.resize(1920, 3000);
        urlSteps.production().path(Pages.OFFER).path(offerBuildingSteps.getId()).open();
        urlSteps.production().path(Pages.MANAGEMENT_NEW).queryParam("status", "published").open();
        refreshUntilSee();
        managementSteps.openOfferStatistic(FIRST);
        Screenshot productionScreenshot = compareSteps.expScreen(
                managementSteps.onManagementNewPage().offer(FIRST).should(isDisplayed()));

        compareSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Step("Обновляем пока не увидим статистику")
    private void refreshUntilSee() {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(500, MILLISECONDS).atMost(100, SECONDS).ignoreExceptions().pollInSameThread()
                .until(() -> {
                    basePageSteps.refresh();
                    managementSteps.onManagementNewPage().offer(FIRST).offerInfo().statsOpener()
                            .waitUntil("Статистика должна быть", isDisplayed(), 1);
                    return true;
                });
    }
}
