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

import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Листинг дилеров - сниппет дилера")
@Feature(DEALERS)
public class ListingSnippetTest {

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbs",
                "mobile/AutoruDealerNew",
                "mobile/SalonPhones",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(NEW).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сниппета")
    public void shouldSeeDealer() {
        basePageSteps.onDealersListingPage().getDealer(0).should(hasText("Автофорум Mercedes-Benz\n" +
                "Официальный дилер Mercedes-Benz\nСеть Major\n784 предложения \nРоссия, Москва и Московская область, " +
                "Москва, внешняя сторона, 92-й километр, МКАД\nПоказать телефон"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по сниппету")
    public void shouldClickDealer() {
        basePageSteps.onDealersListingPage().getDealer(0).click();
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(NEW).path(CARS_OFFICIAL_DEALER).path("/")
                .addParam("from", "dealer-listing-list").shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().info().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «N предложений»")
    public void shouldClickSalesUrl() {
        basePageSteps.onDealersListingPage().getDealer(0).salesUrl().click();
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(NEW).path(CARS_OFFICIAL_DEALER).path("/")
                .addParam("from", "dealer-listing-list").shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().info().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать телефон»")
    public void shouldClickShowPhoneButton() {
        basePageSteps.onDealersListingPage().getDealer(0).showPhoneButton().click();
        basePageSteps.onDealersListingPage().popup().waitUntil(isDisplayed())
                .should(hasText("Телефон\n+7 495 266-44-41\nс 09:00 до 22:00\n+7 495 266-44-42\nс 10:00 до 20:00"));
    }
}
