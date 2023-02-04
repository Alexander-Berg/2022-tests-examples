package ru.auto.tests.cabinet.calls;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.calls.Filters.HIDE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@DisplayName("Фильтры по офферам")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsOfferFiltersDisplayTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff/AllTariffs",
                "cabinet/CommonCustomerGet",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaigns",
                "cabinet/ApiAccessClient",
                "cabinet/Calltracking",
                "cabinet/CalltrackingSettings",
                "cabinet/CalltrackingAggregated").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).open();
        basePageSteps.setWindowSize(1440, 3000);
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот фильтров по офферам")
    public void shouldSeeOfferFiltersScreenshot() {
        basePageSteps.onCallsPage().filters().allParameters().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsPage().filters());

        urlSteps.setProduction().open();
        basePageSteps.onCallsPage().filters().allParameters().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsPage().filters());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются фильтры по офферам по тапу на «Все параметры» под дилером")
    public void shouldSeeOfferFiltersByDealer() {
        basePageSteps.onCallsPage().filters().allParameters().click();

        basePageSteps.onCallsPage().filters().offerFilters().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображаются фильтры по офферам без клика на «Все параметры»")
    public void shouldNotSeeOfferFiltersWithoutAllParametersClick() {
        basePageSteps.onCallsPage().filters().offerFilters().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скрываются фильтры по офферам по тапу на «Свернуть»")
    public void shouldSeeOfferFiltersHide() {
        basePageSteps.onCallsPage().filters().allParameters().click();
        basePageSteps.onCallsPage().filters().button(HIDE).waitUntil(isDisplayed()).click();

        basePageSteps.onCallsPage().filters().offerFilters().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются фильтры по офферам под агенством")
    public void shouldSeeOfferFiltersByAgency() {
        mockRule.overwriteStub(0, "cabinet/Session/AgencyDealerMoscow");
        urlSteps.refresh();
        basePageSteps.onCallsPage().filters().allParameters().click();

        basePageSteps.onCallsPage().filters().offerFilters().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются фильтры по офферам под менеджером")
    public void shouldSeeOfferFiltersByManager() {
        mockRule.overwriteStub(0, "cabinet/Session/Manager");
        urlSteps.refresh();
        basePageSteps.onCallsPage().filters().allParameters().click();

        basePageSteps.onCallsPage().filters().offerFilters().should(isDisplayed());
    }

}
