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

import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Дилеры - поп-ап помощи")
@Feature(DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ListingInfoPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps user;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(NEW).open();
        user.onDealerListingPage().infoPopupButton().hover();
        user.onDealerListingPage().popup().waitUntil(isDisplayed()).waitUntil(hasText("Статус «Официальный дилер» " +
                "используется в значении, указанном в Условиях оказания услуг на сервисе Auto.ru, размещенных по ссылке: " +
                "https://yandex.ru/legal/autoru_cars_dogovor/"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке в поп-апе помощи")
    public void shouldClickPopupUrl() {
        user.onDealerListingPage().popup().button().waitUntil(isDisplayed())
                .should(hasAttribute("href", "https://yandex.ru/legal/autoru_cars_dogovor/")).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}