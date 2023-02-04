package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.BETA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Активное б/у объявление дилера под владельцем")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ActiveSaleUsedDealerOwnerCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "desktop/OfferCarsUsedDealerOwner",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение контактов")
    public void shouldSeeContacts() {
        basePageSteps.onCardPage().contacts().should(hasText("FAVORIT MOTORS Юг\n " +
                "ПражскаяЮжнаяМосква1й Дорожный проезд,д.4 · 189 автомобилей\n+7 495 308-97-71"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onCardPage().cardOwnerPanel().button("Редактировать").click();
        urlSteps.testing().path(BETA).path(CARS).path(USED).path(EDIT).path(SALE_ID).shouldNotSeeDiff();
    }
}
