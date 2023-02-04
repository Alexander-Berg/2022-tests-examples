package ru.auto.tests.desktop.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.DesktopConfig.LISTING_TOP_SALES_CNT;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.KOPITSA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.QueryParams.BADGE;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.SEARCH_TAG;
import static ru.auto.tests.desktop.element.listing.SalesListItem.CIRCULAR_VIEW_CAMERA;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Бейдж «Камера кругового обзора»")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BadgeCircularCameraTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsBadges")
        ).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Бейдж «Камера кругового обзора»")
    public void shouldSeeBadge() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(SEARCH_TAG, BADGE).open();

        basePageSteps.onListingPage().getSale(LISTING_TOP_SALES_CNT).badge(CIRCULAR_VIEW_CAMERA).should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Бейдж «Камера кругового обзора», тип листинга «Карусель»")
    public void shouldSeeBadgeCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).addParam(SEARCH_TAG, BADGE)
                .addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(LISTING_TOP_SALES_CNT).hover();
        basePageSteps.onListingPage().getCarouselSale(LISTING_TOP_SALES_CNT).badge(CIRCULAR_VIEW_CAMERA)
                .should(isDisplayed());
    }

}
