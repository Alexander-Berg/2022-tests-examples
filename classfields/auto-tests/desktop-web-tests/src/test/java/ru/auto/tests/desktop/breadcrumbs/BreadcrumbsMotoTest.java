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
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Хлебные крошки в мото")
@Feature(BREADCRUMBS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BreadcrumbsMotoTest {

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
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferMotoUsedUser"),
                stub("desktop/SearchMotoBreadcrumbsMarkModelUsed"),
                stub("desktop/UserFavoritesAllSubscriptionsEmpty"),
                stub("desktop/ProxyPublicApi")).create();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();

    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение хлебных крошек")
    public void shouldSeeBreadcrumbs() {
        basePageSteps.onCardPage().breadcrumbs().should(hasText("ПродажаМотоциклС пробегомHarley-DavidsonDyna Super Glide " +
                "в Москве"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Продажа»")
    public void shouldClickSellUrl() {
        basePageSteps.onCardPage().breadcrumbs().getItem(0).click();
        urlSteps.testing().path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по категории")
    public void shouldClickCategory() {
        basePageSteps.onCardPage().breadcrumbs().getItem(1).click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по секции")
    public void shouldClickSection() {
        basePageSteps.onCardPage().breadcrumbs().getItem(2).click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке")
    public void shouldClickMark() {
        basePageSteps.onCardPage().breadcrumbs().getItem(3).click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели")
    public void shouldClickModel() {
        basePageSteps.onCardPage().breadcrumbs().getItem(4).click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK).path(MODEL).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке в поп-апе популярных марок")
    public void shouldClickPopularMark() {
        basePageSteps.onCardPage().breadcrumbs().getItem(3).hover();
        basePageSteps.onCardPage().breadcrumbsPopup().waitUntil(isDisplayed());
        String firstMark = basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).getText().toLowerCase()
                .replaceAll(" ", "_");
        basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(firstMark).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по марке в поп-апе всех марок")
    public void shouldClickNonPopularMark() {
        basePageSteps.setWideWindowSize(1024);

        basePageSteps.onCardPage().breadcrumbs().getItem(3).hover();
        basePageSteps.onCardPage().breadcrumbsPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().breadcrumbsPopup().button("Показать все марки").click();
        basePageSteps.onCardPage().breadcrumbsPopup().button("Показать все марки").waitUntil(not(isDisplayed()));
        String firstMark = basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).getText().toLowerCase()
                .replaceAll(" ", "_");
        basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).hover().click();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(firstMark).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели в списке популярных моделей")
    public void shouldClickPopularModel() {
        basePageSteps.setWideWindowSize(1024);

        basePageSteps.onCardPage().breadcrumbs().getItem(4).hover();
        basePageSteps.onCardPage().breadcrumbsPopup().waitUntil(isDisplayed());
        String firstModel = basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).getText().toLowerCase()
                .replaceAll("-", "_").replaceAll("\\s", "_");
        basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK).path(firstModel).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по модели в списке всех моделей")
    public void shouldClickNonPopularModel() {
        basePageSteps.setWideWindowSize(1024);

        basePageSteps.onCardPage().breadcrumbs().getItem(4).hover();
        basePageSteps.onCardPage().breadcrumbsPopup().button("Показать все модели").click();
        basePageSteps.onCardPage().breadcrumbsPopup().button("Показать все модели").waitUntil(not(isDisplayed()));

        String firstModel = basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).getText().toLowerCase()
                .replaceAll("-", "_").replaceAll("\\s", "_");
        basePageSteps.onCardPage().breadcrumbsPopup().getItem(0).hover().click();

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(MARK).path(firstModel).path(USED).shouldNotSeeDiff();
    }
}
