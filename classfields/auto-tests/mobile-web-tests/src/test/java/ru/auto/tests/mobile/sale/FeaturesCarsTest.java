package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Карточка объявления - клик по характеристикам")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FeaturesCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String MARK = "/land_rover/";
    private static final String MODEL = "/discovery/";

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
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/OfferCarsUsedUser",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по году")
    public void shouldClickYear() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().features().feature("год выпуска").button());
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path("/2008-year/").path(USED)
                .shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по кузову")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickBodyType() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().features().feature("Кузов").button());
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(USED)
                .path("/body-allroad_5_doors/").shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по цвету")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickColor() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().features().feature("Цвет").button());
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(USED)
                .path("color-serebristyj/").shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Клик по двигателю")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickEngine() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().features().feature("Двигатель").button());
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(USED)
                .path("/engine-dizel/").shouldNotSeeDiff();
    }
}
