package ru.auto.tests.desktop.specials;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SPECIAL;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Спецпредложения на карточке частника под зарегом")
@Feature(SPECIAL)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SpecialsSaleRegTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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

    //@Parameter("Тип транспорта")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameter(2)
    public String specialsMock;

    @Parameterized.Parameter(3)
    public String lkPath;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser", "desktop/OfferCarsSpecials", CARS},
                {TRUCK, "desktop/OfferTrucksUsedUser", "desktop/OfferTrucksSpecials", TRUCKS},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser", "desktop/OfferMotoSpecials", MOTO}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                saleMock,
                specialsMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().footer(), 0, -1200);
        basePageSteps.onCardPage().specialSales().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переходим на страницу подключения спецпредложения из поп-апа под зарегом")
    public void shouldClickSpecialSalesReg() {
        basePageSteps.onCardPage().specialSales().hover();
        basePageSteps.onCardPage().specialSales().how().hover();
        basePageSteps.onCardPage().activePopupLink().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MY).path(lkPath).addParam("from", "specials_block")
                .addParam("vas_service", "special").shouldNotSeeDiff();
    }
}