package ru.auto.tests.mobile.bestoffer;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BEST_OFFER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GET_BEST_PRICE;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Подбор дилеров с лучшими предложениями")
@Feature(BEST_OFFER)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
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
    public ScreenshotSteps screenshotSteps;

    @Inject
    public UrlSteps urlSteps;

    //@Parameter("Тип ТС")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "mobile/SearchCarsNew",
                "desktop/AuthLoginOrRegister",
                "desktop/UserConfirm",
                "desktop/SearchCarsMarkModelFiltersMatchApplications",
                "mobile/BestOfferTest/SearchCarsBreadcrumbs",
                "mobile/BestOfferTest/SearchCarsCount",
                "mobile/BestOfferTest/SearchCarsNew",
                "mobile/MatchApplications").post();

        urlSteps.testing().path(MOSKVA).path(category).path(NEW).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подбор дилеров с лучшими предложениями")
    public void shouldSendBestOfferApplication() {
        basePageSteps.onListingPage().getGroupSale(0).bestOfferButton().click();
        urlSteps.path(GET_BEST_PRICE).path("/kia/optima/").addParam("from", "listing").shouldNotSeeDiff();
        basePageSteps.onBestOfferPage().select("Kia").should(isDisplayed());
        basePageSteps.onBestOfferPage().select("Optima").should(isDisplayed());
        basePageSteps.onBestOfferPage().input("Номер телефона").click();
        basePageSteps.onBestOfferPage().input("Номер телефона", PHONE);
        basePageSteps.onBestOfferPage().button("Жду предложений").click();
        basePageSteps.onBestOfferPage().input("Код из SMS").waitUntil(isDisplayed());
        basePageSteps.onBestOfferPage().input("Код из SMS", "1234");
        basePageSteps.onBestOfferPage().popup().waitUntil(hasText("Заявка отправлена!\nСкоро наш специалист свяжется " +
                "с вами и подберёт дилеров с лучшими предложениями\nХорошо"));
        basePageSteps.onBestOfferPage().popup().button("Хорошо").click();
        basePageSteps.onBestOfferPage().popup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).path(category).path(NEW).shouldNotSeeDiff();
    }
}
