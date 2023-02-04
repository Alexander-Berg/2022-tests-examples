package ru.auto.tests.desktop.breadcrumbs;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BREADCRUMBS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Хлебные крошки в легковых с пробегом")
@Feature(BREADCRUMBS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
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
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/SearchCarsBreadcrumbsMarkModel"),
                stub("desktop/SearchCarsBreadcrumbsMarkModelGenModification"),
                stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/ProxyPublicApi")).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение хлебных крошек")
    public void shouldSeeBreadcrumbs() {
        basePageSteps.onCardPage().breadcrumbs().should(hasText("Продажа автомобилейС пробегомLand RoverDiscoveryIII" +
                "Внедорожник 5 дв.2.7d AT (190 л.с.) 4WD в Москве"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Продажа автомобилей»")
    public void shouldClickSellUrl() {
        basePageSteps.onCardPage().breadcrumbs().getItem(0).should(hasText("Продажа автомобилей")).click();
        urlSteps.testing().path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по секции")
    public void shouldClickSection() {
        basePageSteps.onCardPage().breadcrumbs().getItem(1).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке")
    public void shouldClickMark() {
        basePageSteps.onCardPage().breadcrumbs().getItem(2).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onCardPage().breadcrumbs().getItem(3).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по поколению")
    public void shouldClickGeneration() {
        basePageSteps.onCardPage().breadcrumbs().getItem(4).click();
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

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке в поп-апе всех марок")
    public void shouldClickNonPopularMark() {
        basePageSteps.setWideWindowSize(1024);

        basePageSteps.onCardPage().breadcrumbs().getItem(2).hover();
        basePageSteps.onCardPage().breadcrumbsPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().breadcrumbsPopup().button("Показать все марки").click();
        basePageSteps.onCardPage().breadcrumbsPopup().button("Показать все марки").waitUntil(not(isDisplayed()));
        String firstMark = basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).getText().toLowerCase();
        basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(firstMark).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели в списке популярных моделей")
    public void shouldClickPopularModel() {
        basePageSteps.setWideWindowSize(1024);

        basePageSteps.onCardPage().breadcrumbs().getItem(3).hover();
        basePageSteps.onCardPage().breadcrumbsPopup().waitUntil(isDisplayed());
        String firstModel = basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).getText().toLowerCase()
                .replaceAll("\\s", "_");
        basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(firstModel).path(USED).shouldNotSeeDiff();
    }
}
