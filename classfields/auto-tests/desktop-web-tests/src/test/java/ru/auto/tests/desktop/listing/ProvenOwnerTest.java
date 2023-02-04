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

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.element.listing.SalesListItem.FROM_OWNER;
import static ru.auto.tests.desktop.element.listing.SalesListItem.FROM_OWNER_BADGE_POPUP;
import static ru.auto.tests.desktop.element.listing.SalesListItem.HOW_PASS_VERIFICATION;
import static ru.auto.tests.desktop.element.listing.SalesListItem.OWNER_HOW_PASS_VERIFICATION_POPUP;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("«От собственника» на сниппете объявления в листинге")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ProvenOwnerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchCarsUsed")
        ).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение бейджа «От собственника»")
    public void shouldSeeProvenOwnerBadge() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).open();

        basePageSteps.onListingPage().getSale(0).badge(FROM_OWNER).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText(FROM_OWNER_BADGE_POPUP));
        basePageSteps.onListingPage().popup().button(HOW_PASS_VERIFICATION).click();

        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText(OWNER_HOW_PASS_VERIFICATION_POPUP));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение бейджа «От собственника», тип листинга «Карусель»")
    public void shouldSeeProvenOwnerBadgeCarousel() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(0).badge(FROM_OWNER).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText(FROM_OWNER_BADGE_POPUP));
        basePageSteps.onListingPage().popup().button(HOW_PASS_VERIFICATION).click();

        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText(OWNER_HOW_PASS_VERIFICATION_POPUP));
    }

}
