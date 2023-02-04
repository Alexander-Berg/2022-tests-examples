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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - объявления с услугами «Поднятие в поиске» и «Выделение цветом»")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasFreshColorListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String searchMock;

    @Parameterized.Parameter(2)
    public String breadcrumbsMock;

    @Parameterized.Parameter(3)
    public String section;

    @Parameterized.Parameter(4)
    public String lkPath;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "mobile/SearchCarsAll", "desktop/SearchCarsBreadcrumbsEmpty", ALL, CARS},
                {TRUCK, "mobile/SearchTrucksAll", "desktop/SearchTrucksBreadcrumbsEmpty", ALL, TRUCKS},
                {MOTORCYCLE, "mobile/SearchMotoAll", "desktop/SearchMotoBreadcrumbsEmpty", ALL, MOTO}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/User",
                searchMock,
                breadcrumbsMock).post();

        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа «Поднятие в поиске»")
    public void shouldSeeFreshPopup() {
        basePageSteps.onListingPage().getSale(5).freshIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().waitUntil(isDisplayed()).should(hasText("ПОДНЯТИЕ В ПОИСКЕ\n" +
                "Самый недорогой способ продвижения, который позволит вам в любой момент оказаться наверху списка " +
                "объявлений, отсортированного по актуальности или по дате. Это поможет быстрее найти покупателя — ведь " +
                "предложения в начале списка просматривают гораздо чаще.\nПерейти в личный кабинет"));
        basePageSteps.onListingPage().popup().closeIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().should(not(isDisplayed()));
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке на личный кабинет в поп-апе «Поднятие в поиске»")
    public void shouldClickFreshPopupUrl() {
        basePageSteps.onListingPage().getSale(5).freshIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().button().waitUntil(isDisplayed()).hover().click();
        urlSteps.testing().path(MY).path(lkPath).addParam("from", "listing")
                .addParam("vas_service", "fresh").shouldNotSeeDiff();
    }

    @Test
    @Owner(SUCHKOVDENIS)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение выделенной цветом цены")
    public void shouldSeeColoredPrice() {
        basePageSteps.onListingPage().getSale(5).coloredPrice().should(isDisplayed());
    }
}
