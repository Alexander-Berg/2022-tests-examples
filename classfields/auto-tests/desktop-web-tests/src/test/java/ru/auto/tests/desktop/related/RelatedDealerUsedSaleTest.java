package ru.auto.tests.desktop.related;

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
import ru.auto.tests.desktop.element.HorizontalCarouselItem;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.RELATED;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(RELATED)
@DisplayName("Похожие на б/у объявлении дилера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class RelatedDealerUsedSaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String RELATED_SALE = "/toyota/land_cruiser/1103691301-d02bfdba/";
    private static final int VISIBLE_SIMILAR_OFFERS = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedDealer",
                "desktop/OfferCarsRelated").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().textbook(), 0, 0);
        basePageSteps.onCardPage().horizontalRelated().waitUntil(isDisplayed()).hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение похожих объявлений")
    public void shouldSeeRelatedSales() {
        basePageSteps.onCardPage().horizontalRelated().itemsList().should(hasSize(5))
                .subList(0, VISIBLE_SIMILAR_OFFERS).forEach(item -> item.waitUntil(isDisplayed()));

        HorizontalCarouselItem sale = basePageSteps.onCardPage().horizontalRelated().getItem(0);
        sale.title().should(hasText("Toyota Land Cruiser"));
        sale.info().should(hasText("2012, 38 000 км"));
        sale.price().should(hasText("2 400 000 ₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по названию дилера")
    public void shouldClickDealerName() {
        basePageSteps.onCardPage().horizontalRelated().dealerUrl().should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(DILER).path(CARS).path(USED).path("/favorit_motors_ug_moskva/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        basePageSteps.onCardPage().horizontalRelated().getItem(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(RELATED_SALE).shouldNotSeeDiff();
    }
}