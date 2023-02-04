package ru.auto.tests.mobile.related;

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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.RELATED;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Похожие на карточке объявления")
@Feature(RELATED)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class RelatedSaleUnregCarsTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/OfferCarsRelated").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().button("Пожаловаться на объявление").hover();
        basePageSteps.onCardPage().related().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение похожих объявлений")
    public void shouldSeeRelatedSales() {
        basePageSteps.onCardPage().related().relatedList().should(hasSize(TOTAL_RELATED_OFFERS_COUNT))
                .subList(0, VISIBLE_RELATED_OFFERS_COUNT).forEach(item -> item.waitUntil(isDisplayed()));

        basePageSteps.onCardPage().related().title().should(hasText("Похожие объявления"));
        basePageSteps.onCardPage().related().getRelated(0).should(hasText("Нижний Новгород\n2 400 000 ₽\n" +
                "Toyota Land Cruiser 200 Series Рестайлинг 1\n2012 г., 38 000 км"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickRelatedSale() {
        mockRule.with("desktop/OfferCarsUsedUser2").update();

        basePageSteps.onCardPage().related().getRelated(0).click();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/toyota/land_cruiser/1103691301-d02bfdba/")
                .shouldNotSeeDiff();
    }
}
