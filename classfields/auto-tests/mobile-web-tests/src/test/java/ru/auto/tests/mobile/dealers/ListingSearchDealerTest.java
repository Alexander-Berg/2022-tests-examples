package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Листинг дилеров - поиск по названию")
@Feature(DEALERS)
public class ListingSearchDealerTest {

    private static final String DEALER_ID = "21510191";
    private static final String DEALER_NAME = "РОЛЬФ Фольксваген Центр Север | Автомобили с пробегом";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поиск по имени дилера")
    public void shouldSearchByDealerName() {
        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(ALL).open();

        basePageSteps.onDealersListingPage().input("Название дилера", DEALER_NAME);
        basePageSteps.onDealersListingPage().suggestItem(DEALER_NAME).click();
        basePageSteps.onDealersListingPage().dealersList().waitUntil(hasSize(1));
        basePageSteps.onDealersListingPage().getDealer(0).name().waitUntil(hasText(DEALER_NAME)).click();

        basePageSteps.onDealerCardPage().info().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Очистка имени дилера")
    public void shouldClearDealerName() {
        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(ALL).addParam("dealer_id", DEALER_ID).open();

        int size = basePageSteps.onDealersListingPage().dealersList().size();

        basePageSteps.onDealersListingPage().clearInput().click();
        basePageSteps.onDealersListingPage().input("Название дилера").waitUntil(hasValue(""));

        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(ALL).ignoreParam("cookiesync").shouldNotSeeDiff();
        basePageSteps.onDealersListingPage().dealersList().waitUntil(hasSize(greaterThan(size)));
    }
}
