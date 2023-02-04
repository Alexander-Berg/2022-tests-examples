package ru.auto.tests.mobile.specials;

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
import static ru.auto.tests.desktop.consts.AutoruFeatures.SPECIAL;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Спецпредложения на карточке объявления")
@Feature(SPECIAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
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
        basePageSteps.onCardPage().button("Пожаловаться на объявление").hover();
        basePageSteps.onCardPage().specials().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение спецпредложений")
    public void shouldSeeSpecials() {
        basePageSteps.onCardPage().specials().specialsList().should(hasSize(TOTAL_SPECIAL_SALES_COUNT))
                .subList(0, VISIBLE_SPECIAL_SALES_COUNT).forEach(item -> item.waitUntil(isDisplayed()));

        basePageSteps.onCardPage().specials().title().waitUntil(hasText("Спецпредложения"));
        basePageSteps.onCardPage().specials().getSpecial(0)
                .waitUntil(hasText("Новосибирск\n380 000 ₽\nГАЗ ГАЗель Next 4.6\n1969 г., 1 353 км"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        mockRule.with("desktop/OfferTrucksUsedUser2").update();

        basePageSteps.scrollAndClick(basePageSteps.onCardPage().specials().getSpecial(0));
        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path("/gaz/gazel_next_4_6/19168121-abb524a9/")
                .shouldNotSeeDiff();
    }
}
