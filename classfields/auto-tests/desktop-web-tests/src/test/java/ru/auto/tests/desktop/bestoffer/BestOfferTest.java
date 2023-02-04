package ru.auto.tests.desktop.bestoffer;

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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BEST_OFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Подбор дилеров с лучшими предложениями")
@Feature(BEST_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class BestOfferTest {

    private static final String PHONE = "9111111111";

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsNew",
                "desktop/AuthLoginOrRegister",
                "desktop/UserConfirm",
                "desktop/SearchCarsMarkModelFiltersMatchApplications",
                "desktop/BestOfferTest/SearchCarsBreadcrumbs",
                "desktop/BestOfferTest/SearchCarsCount",
                "desktop/BestOfferTest/MatchApplications").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подбор дилеров с лучшими предложениями")
    public void shouldSendBestOfferApplication() {
        basePageSteps.onListingPage().getGroupSale(0).bestOfferButton().click();
        basePageSteps.onListingPage().bestOfferPopup().select("Kia").should(isDisplayed());
        basePageSteps.onListingPage().bestOfferPopup().select("Optima").should(isDisplayed());
        basePageSteps.onListingPage().bestOfferPopup().input("Номер телефона").click();
        basePageSteps.onListingPage().bestOfferPopup().input("Номер телефона", PHONE);
        basePageSteps.onListingPage().bestOfferPopup().button("Жду предложений").click();
        basePageSteps.onListingPage().bestOfferPopup().input("Код из SMS").waitUntil(isDisplayed());
        basePageSteps.onListingPage().bestOfferPopup().input("Код из SMS", "1234");
        basePageSteps.onListingPage().bestOfferPopup().result().waitUntil(hasText("Заявка отправлена!\nСкоро наш " +
                "специалист свяжется с вами и подберёт дилеров с лучшими предложениями"));
        basePageSteps.onListingPage().bestOfferPopup().button("Хорошо").click();
        basePageSteps.onListingPage().bestOfferPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().getGroupSale(0).bestOfferButton().waitUntil(not(isDisplayed()));
    }
}