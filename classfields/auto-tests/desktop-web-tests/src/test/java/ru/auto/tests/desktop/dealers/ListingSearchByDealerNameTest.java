package ru.auto.tests.desktop.dealers;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Дилеры - поиск по названию")
@Feature(DEALERS)
public class ListingSearchByDealerNameTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Поиск по имени дилера")
    public void shouldSearchByDEALER_NAME() {
        String dealerName = "Панавто Mercedes-Benz МКАД";
        basePageSteps.onDealerListingPage().nameInput().sendKeys(dealerName);
        basePageSteps.onDealerListingPage().suggestItem(dealerName).click();
        basePageSteps.onDealerListingPage().dealerList().should(hasSize(1)).get(0).name()
                .waitUntil(hasText(dealerName));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Очистка имени дилера")
    public void shouldClearDealerName() {
        String dealerId = "20135120";
        urlSteps.addParam("dealer_id", dealerId).open();
        int size = basePageSteps.onDealerListingPage().dealerList().should(hasSize(greaterThan(0))).size();
        basePageSteps.onDealerListingPage().inputClear().click();
        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(ALL).shouldNotSeeDiff();
        basePageSteps.onDealerListingPage().dealerList().waitUntil(hasSize(greaterThan(size)));
    }
}
