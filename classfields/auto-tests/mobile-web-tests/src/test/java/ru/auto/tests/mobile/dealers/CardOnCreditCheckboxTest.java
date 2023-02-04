package ru.auto.tests.mobile.dealers;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.CREDITS;
import static ru.auto.tests.desktop.consts.Owners.NIKOVCHARENKO;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Чекбокс «В кредит»")
@Feature(CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CardOnCreditCheckboxTest {

    private static final String DEALER_CODE = "/inchcape_certified_moskva/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SalonNotOfficial",
                "desktop/SearchCarsCountDealerIdNotOfficial",
                "desktop/SearchCarsMarkModelFiltersAllDealerIdSeveralMarks",
                "desktop/SearchCarsMarkModelFiltersNewDealerIdSeveralMarks",
                "desktop/SearchCarsMarkModelFiltersUsedDealerIdSeveralMarks",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(DILER).path(CARS).path(USED).path(DEALER_CODE).open();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выставляем чекбокс «В кредит»")
    public void shouldTurnCheckboxOn() {
        basePageSteps.hideElement(basePageSteps.onDealerCardPage().button("Позвонить"));
        basePageSteps.onDealerCardPage().filters().checkbox("В кредит").click();

        urlSteps.addParam("on_credit", "true").shouldNotSeeDiff();
        basePageSteps.onListingPage().waitForListingReload();
        basePageSteps.onDealerCardPage().filters().should(isDisplayed());
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class, Testing.class})
    @DisplayName("Снимаем чекбокс «В кредит»")
    public void shouldTurnCheckboxOff() {
        urlSteps.addParam("on_credit", "true").open();
        basePageSteps.hideElement(basePageSteps.onDealerCardPage().button("Позвонить"));
        basePageSteps.onDealerCardPage().filters().checkbox("В кредит").click();

        urlSteps.testing().path(DILER).path(CARS).path(USED).path(DEALER_CODE).shouldNotSeeDiff();
    }

    @Test
    @Owner(NIKOVCHARENKO)
    @Category({Regression.class, Testing.class})
    @DisplayName("Сброс параметра при переходе в новые")
    public void shouldResetParamInNew() {
        urlSteps.addParam("on_credit", "true").open();
        basePageSteps.onDealerCardPage().filters().section("Новые").click();
        urlSteps.testing().path(DILER).path(CARS).path(NEW).path(DEALER_CODE).shouldNotSeeDiff();
    }
}
