package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Листинг - гео-кольца")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GeoRadiusCountersCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsAll",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsOfferLocatorCountersTotalCount",
                "desktop/SearchCarsOfferLocatorCountersTotalCountGeoRadius300",
                "desktop/SearchCarsAllGeoRadius300").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по гео-кольцу")
    public void shouldClickGeoRadiusCounter() {
        basePageSteps.onListingPage().geoRadiusCounters().should(hasText("Москва\nНет предложений\n+ 200 км\n" +
                "2 предложения\n+ 300 км\n29 694 предложения\n+ 400 км\n41 334 предложения\n+ 500 км\n" +
                "50 896 предложений\n+ 1000 км\n87 529 предложений"));
        basePageSteps.onListingPage().geoRadiusCounters().getGeoRadiusCounter(1)
                .should(hasClass(containsString("ListingGeoRadiusCounters__item_active")));
        basePageSteps.onListingPage().geoRadiusCounters().getGeoRadiusCounter(2).click();
        urlSteps.shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue("gradius", "300");
        basePageSteps.onListingPage().geoRadiusCounters().getGeoRadiusCounter(2)
                .waitUntil(hasClass(containsString("ListingGeoRadiusCounters__item_active")));
        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }
}