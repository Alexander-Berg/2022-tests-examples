package ru.auto.tests.desktop.header;

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

import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Карточка - подшапка")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SubHeaderSaleCommerceTest {

    private static final String SALE_ID = "1076842087-f1e84";
    private static final String MARK = "zil";
    private static final String MODEL = "5301";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferTrucksUsedUser").post();

        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(SALE_ID).open();

    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке «Объявление»")
    public void shouldClickSaleTab() {
        basePageSteps.onCardPage().subHeader().button("Объявление").click();
        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path(MARK).path(MODEL).path(SALE_ID).path("/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по вкладке «Отзывы»")
    public void shouldClickReviewsTab() {
        basePageSteps.onCardPage().subHeader().button("Отзывы").should(hasAttribute("href",
                        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(TRUCK).path(MARK).path(MODEL).path("/").toString()))
                .click();
        urlSteps.shouldUrl(startsWith(urlSteps.testing().path(REVIEWS).path(TRUCKS).toString()));
    }
}