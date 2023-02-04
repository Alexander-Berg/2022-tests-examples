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
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Спецпредложения на карточке частника")
@Feature(SPECIAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SpecialsSaleUnregTrucksTest {

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
        mockRule.newMock().with("desktop/OfferTrucksUsedUser",
                "desktop/OfferTrucksSpecials").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().footer(), 0, -300);
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
                .waitUntil(hasText("ГАЗ ГАЗель Next 4.6\n1969, 1 353 км\n380 000 ₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Подключить» в поп-апе описания услуги")
    public void shouldClickActivateButton() {
        basePageSteps.onCardPage().specialSales().waitUntil(isDisplayed());
        basePageSteps.onCardPage().specialSales().how().should(isDisplayed()).hover();
        basePageSteps.onCardPage().activePopupLink().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MY).path(TRUCKS).addParam("from", "specials_block")
                .addParam("vas_service", "special").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        mockRule.with("desktop/OfferTrucksUsedUser2").update();

        basePageSteps.onCardPage().specialSales().getItem(0).should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path("/gaz/gazel_next_4_6/19168121-abb524a9/")
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
