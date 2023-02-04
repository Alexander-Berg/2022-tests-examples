package ru.auto.tests.desktop.specials;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SPECIAL;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Спецпредложения на карточке частника")
@Feature(SPECIAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SpecialsSaleUnregCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final int TOTAL_SPECIAL_SALES_COUNT = 5;
    private static final int VISIBLE_SPECIAL_SALES_COUNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/OfferCarsSpecials").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().footer(), 0, -1200);
        basePageSteps.onCardPage().specialSales().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение спецпредложений")
    public void shouldSeeSpecialSales() {
        basePageSteps.onCardPage().specialSales().itemsList().should(hasSize(TOTAL_SPECIAL_SALES_COUNT))
                .subList(0, VISIBLE_SPECIAL_SALES_COUNT).forEach(item -> item.waitUntil(isDisplayed()));

        basePageSteps.onCardPage().specialSales().title().waitUntil(hasText("Спецпредложения"));
        basePageSteps.onCardPage().specialSales().getItem(0)
                .waitUntil(hasText("Toyota Land Cruiser\n2012, 38 000 км\n2 400 000 \u20BD"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Подключить» в поп-апе описания услуги")
    public void shouldClickActivateButton() {
        basePageSteps.onCardPage().specialSales().waitUntil(isDisplayed());
        basePageSteps.onCardPage().specialSales().how().should(isDisplayed()).hover();
        basePageSteps.onCardPage().activePopupLink().hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MY).path(CARS).addParam("from", "specials_block")
                .addParam("vas_service", "special").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        mockRule.with("desktop/OfferCarsUsedUser2").update();

        basePageSteps.onCardPage().specialSales().getItem(0).should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/toyota/land_cruiser/1103691301-d02bfdba/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Листание объявлений")
    public void shouldSlideSales() {
        basePageSteps.onCardPage().specialSales().itemsList().subList(0, VISIBLE_SPECIAL_SALES_COUNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onCardPage().specialSales().prevButton().should(not(isDisplayed()));
        basePageSteps.onCardPage().specialSales().nextButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().specialSales().itemsList().subList(0, VISIBLE_SPECIAL_SALES_COUNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onCardPage().specialSales().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().specialSales().itemsList().subList(0, VISIBLE_SPECIAL_SALES_COUNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onCardPage().specialSales().nextButton().waitUntil(isDisplayed());
        basePageSteps.onCardPage().specialSales().prevButton().should(not(isDisplayed()));
    }
}
