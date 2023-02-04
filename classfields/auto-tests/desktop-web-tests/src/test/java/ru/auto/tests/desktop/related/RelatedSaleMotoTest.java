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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.RELATED;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Похожие объявления на карточке объявления")
@Feature(RELATED)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class RelatedSaleMotoTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final int TOTAL_RELATED_OFFERS_COUNT = 5;
    private static final int VISIBLE_RELATED_OFFERS_COUNT = 4;

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
        mockRule.newMock().with("desktop/OfferMotoUsedUser",
                "desktop/OfferMotoRelated").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().footer(), 0, -1200);
        basePageSteps.onCardPage().horizontalRelated().waitUntil(isDisplayed()).hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение похожих объявлений")
    public void shouldSeeRelatedSales() {
        basePageSteps.onCardPage().horizontalRelated().itemsList().should(hasSize(TOTAL_RELATED_OFFERS_COUNT))
                .subList(0, VISIBLE_RELATED_OFFERS_COUNT).forEach(item -> item.waitUntil(isDisplayed()));

        basePageSteps.onCardPage().horizontalRelated().title().should(hasText("Похожие объявления"));
        basePageSteps.onCardPage().horizontalRelated().getItem(0).should(hasText("Honda CBR 300R\n" +
                "2009, 22 000 км\n340 000 ₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Листание похожих")
    public void shouldScrollRelatedSales() {
        basePageSteps.onCardPage().horizontalRelated().itemsList().subList(0, VISIBLE_RELATED_OFFERS_COUNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onCardPage().horizontalRelated().prevButton().should(not(isDisplayed()));
        basePageSteps.onCardPage().horizontalRelated().nextButton().click();
        basePageSteps.onCardPage().horizontalRelated().itemsList().subList(0, VISIBLE_RELATED_OFFERS_COUNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onCardPage().horizontalRelated().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().horizontalRelated().itemsList().subList(0, VISIBLE_RELATED_OFFERS_COUNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onCardPage().horizontalRelated().nextButton().waitUntil(isDisplayed());
        basePageSteps.onCardPage().horizontalRelated().prevButton().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению в похожих")
    public void shouldClickRelatedSale() {
        mockRule.with("desktop/OfferMotoUsedUser2").update();

        basePageSteps.onCardPage().horizontalRelated().getItem(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path("/honda/cbr_300/4102039-a52db929/")
                .shouldNotSeeDiff();
    }
}
