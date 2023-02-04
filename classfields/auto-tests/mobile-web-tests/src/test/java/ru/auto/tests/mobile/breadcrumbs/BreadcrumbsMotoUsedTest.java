package ru.auto.tests.mobile.breadcrumbs;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.CardPageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BREADCRUMBS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Хлебные крошки в мото")
@Feature(BREADCRUMBS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class BreadcrumbsMotoUsedTest {

    private static final String SALE_ID = "1076842087-f1e84";
    private static final String MARK = "harley_davidson";
    private static final String MODEL = "dyna_super_glide";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private CardPageSteps cardPageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferMotoUsedUser"),
                stub("desktop/SearchMotoBreadcrumbsMarkModelUsed"),
                stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение крошек")
    public void shouldSeeBreadcrumbs() {
        basePageSteps.onCardPage().breadcrumbs().should(hasText("ПродажаМотоциклС пробегомHarley-DavidsonDyna Super " +
                "Glide в Москве"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Продажа»")
    public void shouldClickSellUrl() {
        cardPageSteps.clickBreadcrumbItem(0);

        urlSteps.testing().path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по категории")
    public void shouldClickCategory() {
        cardPageSteps.clickBreadcrumbItem(1);

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по секции")
    public void shouldClickSection() {
        cardPageSteps.clickBreadcrumbItem(2);

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке")
    public void shouldClickMark() {
        cardPageSteps.clickBreadcrumbItem(3);

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onCardPage().breadcrumbs().getItem(4).button().click();

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK).path(MODEL).path(USED).shouldNotSeeDiff();
    }
}