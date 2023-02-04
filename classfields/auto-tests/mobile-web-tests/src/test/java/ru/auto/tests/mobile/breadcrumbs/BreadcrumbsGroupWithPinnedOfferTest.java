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

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BREADCRUMBS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Хлебные крошки на групповой карточке c запиненным оффером")
@Feature(BREADCRUMBS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class BreadcrumbsGroupWithPinnedOfferTest {

    private static final String PATH = "/kia/optima/21342125/21342344/1076842087-f1e84/";
    private static final String MARK = "kia";
    private static final String MODEL = "optima";
    private static final String CONFIGURATION = "/21342050-21342121/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public CardPageSteps cardPageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsNewDealer"),
                stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение хлебных крошек")
    public void shouldSeeBreadcrumbs() {
        basePageSteps.onCardPage().breadcrumbs()
                .should(hasText("Продажа автомобилейНовыйKiaOptimaIV РестайлингСедан2.0 AT (150 л.с.) в Москве"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по секции")
    public void shouldClickSection() {
        cardPageSteps.clickBreadcrumbItem(1);

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке")
    public void shouldClickMark() {
        cardPageSteps.clickBreadcrumbItem(2);

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(NEW).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        cardPageSteps.clickBreadcrumbItem(3);

        urlSteps.shouldUrl(anyOf(
                equalTo(urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW).toString()),
                equalTo(urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL)
                        .path(CONFIGURATION).addParam("from", "single_group_snippet_listing")
                        .toString())));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по поколению")
    public void shouldClickGeneration() {
        cardPageSteps.clickBreadcrumbItem(4);

        urlSteps.shouldUrl(anyOf(
                equalTo(urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW).toString()),
                equalTo(urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL)
                        .path(CONFIGURATION).addParam("from", "single_group_snippet_listing")
                        .toString())));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кузову")
    public void shouldClickBodyType() {
        basePageSteps.onCardPage().breadcrumbs().getItem(5).click();

        urlSteps.shouldUrl(anyOf(
                equalTo(urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW).toString()),
                equalTo(urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL)
                        .path(CONFIGURATION).addParam("from", "single_group_snippet_listing")
                        .toString())));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модификации")
    public void shouldClickModification() {
        basePageSteps.onCardPage().breadcrumbs().getItem(6).click();

        urlSteps.shouldUrl(anyOf(
                equalTo(urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW).toString()),
                equalTo(urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL)
                        .path(CONFIGURATION).addParam("from", "single_group_snippet_listing")
                        .toString())));
    }
}
