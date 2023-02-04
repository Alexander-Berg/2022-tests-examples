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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SPECIAL;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Спецпредложения в статистике")
@Feature(SPECIAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SpecialsStatsTest {

    private static final int VISIBLE_ITEMS_CNT = 4;

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
        mockRule.newMock().with("desktop/SearchCarsSpecials").post();

        urlSteps.testing().path(STATS).path(CARS).path("/vaz/").open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onStatsPage().footer(), 0, 0);
        basePageSteps.onBasePage().specialSales().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поп-ап с описанием услуги")
    public void shouldSeePopup() {
        basePageSteps.onBasePage().specialSales().how().waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed())
                .should(hasText("Спецпредложение\nВаше объявление будет отображаться в специальном блоке в результатах " +
                        "поиска и на карточках объявлений о продаже аналогичных авто. А для легковых — также на главной " +
                        "странице и в Каталоге.\n5\nУвеличивает количество просмотров в 5 раз\nПодключить у себя"
                ));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка в поп-апе")
    public void shouldClickPopupUrl() {
        basePageSteps.onBasePage().specialSales().waitUntil(isDisplayed());
        basePageSteps.onBasePage().specialSales().how().waitUntil(isDisplayed()).hover();
        basePageSteps.onBasePage().activePopupLink().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MY).path(ALL).addParam("from", "stats")
                .addParam("vas_service", "special").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Листание объявлений")
    public void shouldSlideSales() {
        basePageSteps.onBasePage().specialSales().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onBasePage().specialSales().prevButton().should(not(isDisplayed()));
        basePageSteps.onBasePage().specialSales().nextButton().waitUntil(isDisplayed()).click();
        basePageSteps.onBasePage().specialSales().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onBasePage().specialSales().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onBasePage().specialSales().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.should(isDisplayed()));
        basePageSteps.onBasePage().specialSales().nextButton().waitUntil(isDisplayed());
        basePageSteps.onBasePage().specialSales().prevButton().should(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSpecialSale() {
        basePageSteps.onBasePage().specialSales().getItem(0).should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/nissan/note/1093704908-cce4e780/")
                .shouldNotSeeDiff();
    }
}