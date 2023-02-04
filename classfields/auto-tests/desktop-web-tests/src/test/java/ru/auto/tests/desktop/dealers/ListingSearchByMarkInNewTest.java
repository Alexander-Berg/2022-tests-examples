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
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Дилеры - поиск по марке в новых")
@Feature(DEALERS)
public class ListingSearchByMarkInNewTest {

    private final static String MARK = "Audi";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(NEW).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение марок")
    public void shouldSeeMarks() {
        basePageSteps.onDealerListingPage().searchBlock().marks().should(isDisplayed());
        basePageSteps.onDealerListingPage().searchBlock().marksList().should(hasSize(greaterThan(0)));
        basePageSteps.onDealerListingPage().searchBlock().getMark(0).should(hasText("LADA (ВАЗ)"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по марке")
    public void shouldClickMark() {
        basePageSteps.onDealerListingPage().searchBlock().mark(MARK).click();
        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(MARK.toLowerCase()).path(NEW)
                .addParam("dealer_org_type", "1").shouldNotSeeDiff();
        basePageSteps.onDealerListingPage().dealerList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onDealerListingPage().map().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение дилеров после клика по марке")
    public void shouldSeeDealers() {
        basePageSteps.onDealerListingPage().searchBlock().mark(MARK).click();
        basePageSteps.onDealerListingPage().dealerList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onDealerListingPage().searchBlock().marks().should(not(isDisplayed()));
    }
}

