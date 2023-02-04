package ru.auto.tests.desktop.listing;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - скрытие объявлений")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HiddenSaleTest {

    private final static String HIDDEN_COOKIE = "hidden-sales";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String listingMock;

    @Parameterized.Parameter(2)
    public String breadcrumbsMock;

    @Parameterized.Parameter(3)
    public String hiddenSaleText;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/SearchCarsAll", "desktop/SearchCarsBreadcrumbsEmpty",
                        "Объявление скрыто\nLifan Solano II\n500 000 ₽\n2020\n100 км"},
                {TRUCK, "desktop/SearchTrucksAll", "desktop/SearchTrucksBreadcrumbsEmpty",
                        "Объявление скрыто\nКамАЗ 65117\n2 490 000 ₽\n2012\n53 270 км"},
                {MOTORCYCLE, "desktop/SearchMotoAll", "desktop/SearchMotoBreadcrumbsEmpty",
                        "Объявление скрыто\nHarley-Davidson Sportster 1200\nЧоппер\n625 000 ₽\n2013\n18 000 км"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub(listingMock),
                stub(breadcrumbsMock)
        ).create();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Скрытие объявления")
    public void shouldSeeHiddenSale() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().getSale(0).toolBar().hideButton().waitUntil(isDisplayed())
                .click();
        basePageSteps.onListingPage().getSale(0).waitUntil(hasText(hiddenSaleText));
        cookieSteps.shouldSeeCookieWithValue(HIDDEN_COOKIE,
                basePageSteps.getOfferId(basePageSteps.onListingPage().getSale(0).nameLink()));

        basePageSteps.onListingPage().getSale(0).toolBar().showButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().getSale(0).nameLink().waitUntil(isDisplayed());
        cookieSteps.shouldSeeCookieWithValue(HIDDEN_COOKIE, "");
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Скрытие объявления, тип листинга «Карусель»")
    public void shouldSeeHiddenSaleCarousel() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam(OUTPUT_TYPE, CAROUSEL).open();
        basePageSteps.onListingPage().hiddenCarouselSalesList().waitUntil(hasSize(0));

        String offerId = basePageSteps.getOfferId(basePageSteps.onListingPage().getCarouselSale(0).link());

        basePageSteps.onListingPage().getCarouselSale(0).hover();
        basePageSteps.onListingPage().getCarouselSale(0).toolBar().hideButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().hiddenCarouselSalesList().waitUntil(hasSize(1));
        cookieSteps.shouldSeeCookieWithValue(HIDDEN_COOKIE, offerId);

        basePageSteps.onListingPage().hiddenCarouselSalesList().get(0).unfold().waitUntil(isDisplayed()).click();
        cookieSteps.shouldSeeCookieWithValue(HIDDEN_COOKIE, "");
    }

}
