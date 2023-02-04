package ru.auto.tests.desktop.sale;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.*;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Такой же, но новый")
@Feature(AutoruFeatures.SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
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
        basePageSteps.scrollDown(1500);
        basePageSteps.onCardPage().sameButNew().waitUntil(isDisplayed()).hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeSameButNewBlock() {
        basePageSteps.onCardPage().sameButNew().title().should(hasText("Такой же, но новый"));
        basePageSteps.onCardPage().sameButNew().itemsList().should(hasSize(OFFERS_COUNT));
        basePageSteps.onCardPage().sameButNew().getItem(0).should(hasText("Audi Q3 40 TFSI II (F3)\nSport 40 TFSI " +
                "quattro S tronic / 2.0 л / AMT / 180 л.с.\n3 630 000 ₽"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        mockRule.overwriteStub(1, "desktop/OfferCarsNewDealer");

        basePageSteps.onCardPage().sameButNew().getItem(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path("/21680441/21680460/")
                .path(SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать все объявления»")
    public void shouldClickShowAllSalesButton() {
        basePageSteps.onCardPage().sameButNew().button("Показать все объявления").click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW).shouldNotSeeDiff();
    }
}
