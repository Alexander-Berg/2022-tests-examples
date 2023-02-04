package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Такой же, но новый")
@Feature(AutoruFeatures.SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class SameButNewTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final int OFFERS_COUNT = 4;
    private static final String MARK = "audi";
    private static final String MODEL = "q3";

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
        mockRule.newMock().with("desktop/OfferCarsUsedUserSameButNew",
                "desktop/SearchCarsContextSameButNew").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().sellerComment().hover();
        basePageSteps.onCardPage().sameButNew().waitUntil(isDisplayed()).hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeSameButNewBlock() {
        basePageSteps.onCardPage().sameButNew().title().should(hasText("Такой же, но новый"));
        basePageSteps.onCardPage().sameButNew().salesList().should(hasSize(OFFERS_COUNT));
        basePageSteps.onCardPage().sameButNew().getSale(0).should(hasText("Audi Q3 40 TFSI II (F3)\nSport 40 TFSI " +
                "quattro S tronic / 2.0 л / AMT / 180 л.с.\n3 630 000 ₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        mockRule.overwriteStub(1, "desktop/OfferCarsNewDealer");

        basePageSteps.onCardPage().sameButNew().getSale(0).click();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path("/21680441/21680460/")
                .path(SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать все объявления»")
    public void shouldClickShowAllSalesButton() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().sameButNew().button("Все объявления"));
        urlSteps.shouldUrl(anyOf(
                equalTo(urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW).toString()),
                equalTo(urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL)
                        .path("/21356775-21356854/").addParam("from", "single_group_snippet_listing")
                        .toString())));
    }
}
