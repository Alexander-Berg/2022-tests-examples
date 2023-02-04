package ru.auto.tests.mobile.callback;

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
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CALLBACK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.mobile.page.ListingPage.TOP_SALES_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Заказ обратного звонка в галерее сниппета под зарегом")
@Feature(CALLBACK)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallbackListingSnippetGalleryTest {

    private static final String PHONE = "79111111111";

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String searchMock;

    @Parameterized.Parameter(2)
    public String breadcrumbsMock;

    @Parameterized.Parameter(3)
    public String phonesMock;

    @Parameterized.Parameter(4)
    public String callbackMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "mobile/SearchCarsAll", "desktop/SearchCarsBreadcrumbsEmpty", "desktop/OfferCarsPhones",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"},
                {TRUCK, "desktop/SearchTrucksBreadcrumbsEmpty", "mobile/SearchTrucksAll", "desktop/OfferTrucksPhones",
                        "desktop/OfferTrucksRegisterCallback"},
                {MOTORCYCLE, "mobile/SearchMotoAll", "desktop/SearchMotoBreadcrumbsEmpty", "desktop/OfferMotoPhones",
                        "desktop/OfferMotoRegisterCallback"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                searchMock,
                breadcrumbsMock,
                phonesMock,
                callbackMock).post();

        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заказ обратного звонка под зарегом")
    public void shouldRequestCallback() {
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT).hover();
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT).gallery().contacts().click();
        basePageSteps.onListingPage().popup().button("Заказать обратный звонок").click();
        basePageSteps.onListingPage().callbackPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().callbackPopup().button("Перезвоните мне").waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().notifier().waitUntil(hasText("Заявка отправлена"));
        basePageSteps.onListingPage().callbackPopup().waitUntil(not(isDisplayed()));
    }
}
