package ru.auto.tests.mobile.vas;

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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mobile.page.ListingPage.TOP_SALES_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - объявления с услугой «Топ»")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasTopListingTest {

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String searchMock;

    @Parameterized.Parameter(2)
    public String breadcrumbsMock;

    @Parameterized.Parameter(3)
    public String phonesMock;

    @Parameterized.Parameter(4)
    public String section;

    @Parameterized.Parameter(5)
    public String lkCategory;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "mobile/SearchCarsAll", "desktop/SearchCarsBreadcrumbsEmpty", "desktop/OfferCarsPhones",
                        ALL, CARS},
                {TRUCK, "mobile/SearchTrucksAll", "desktop/SearchTrucksBreadcrumbsEmpty", "desktop/OfferTrucksPhones",
                        ALL, TRUCKS},
                {MOTORCYCLE, "mobile/SearchMotoAll", "desktop/SearchMotoBreadcrumbsEmpty", "desktop/OfferMotoPhones",
                        ALL, MOTO}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/User",
                searchMock,
                breadcrumbsMock,
                phonesMock).post();

        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение объявлений с услугой ТОП")
    public void shouldSeeTopSales() {
        basePageSteps.onListingPage().topSalesList().should(hasSize(TOP_SALES_COUNT))
                .forEach(item -> item.topIcon().should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поп-ап с описанием услуги")
    public void shouldSeePopup() {
        basePageSteps.onListingPage().topSalesList().should(hasSize(TOP_SALES_COUNT)).get(0).topIcon().click();
        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText("ПОДНЯТИЕ В ТОП\nВаше объявление " +
                "окажется в специальном блоке на самом верху списка при сортировке по актуальности или по дате. " +
                "Покупатели вас точно не пропустят.\nПерейти в личный кабинет"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке в поп-апе")
    public void shouldClickPopupUrl() {
        basePageSteps.onListingPage().topSalesList().should(hasSize(TOP_SALES_COUNT)).get(0).topIcon().click();
        basePageSteps.onListingPage().popup().button().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MY).path(lkCategory).addParam("from", "listing")
                .addParam("vas_service", "top").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр телефонов")
    public void shouldSeePhones() {
        basePageSteps.onListingPage().getSale(0).callButton().click();
        basePageSteps.onListingPage().popup().waitUntil(hasText("Телефон\n+7 916 039-84-27\nс 10:00 до 23:00\n" +
                "+7 916 039-84-28\nс 12:00 до 20:00"));
    }
}
