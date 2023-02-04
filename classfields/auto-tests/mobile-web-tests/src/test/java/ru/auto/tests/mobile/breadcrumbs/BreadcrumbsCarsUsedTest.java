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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Хлебные крошки в легковых б/у")
@Feature(BREADCRUMBS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class BreadcrumbsCarsUsedTest {

    private static final String SALE_ID = "1076842087-f1e84";
    private static final String MARK = "land_rover";
    private static final String MODEL = "discovery";
    private static final String GENERATION = "2307388";
    private static final String BODY = "2307389";
    private static final String MODIFICATION = "2307392";

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
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/SearchCarsBreadcrumbsMarkModel"),
                stub("desktop/SearchCarsBreadcrumbsMarkModelGenModification"),
                stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/ProxyPublicApi")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение крошек")
    public void shouldSeeBreadcrumbs() {
        basePageSteps.onCardPage().breadcrumbs().should(hasText("Продажа автомобилейС пробегомLand RoverDiscoveryIII" +
                "Внедорожник 5 дв.2.7d AT (190 л.с.) 4WD в Москве"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Продажа автомобилей»")
    public void shouldClickSellUrl() {
        cardPageSteps.clickBreadcrumbItem(0);

        urlSteps.testing().path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по секции")
    public void shouldClickSection() {
        cardPageSteps.clickBreadcrumbItem(1);

        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке")
    public void shouldClickMark() {
        cardPageSteps.clickBreadcrumbItem(2);

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        cardPageSteps.clickBreadcrumbItem(3);

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по поколению")
    public void shouldClickGeneration() {
        cardPageSteps.clickBreadcrumbItem(4);

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(USED)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кузову")
    public void shouldClickBodyType() {
        basePageSteps.onCardPage().breadcrumbs().getItem(5).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(BODY).path(USED)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модификации")
    public void shouldClickModification() {
        basePageSteps.onCardPage().breadcrumbs().getItem(6).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(BODY)
                .path(MODIFICATION).path(USED).shouldNotSeeDiff();
    }
}