package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.element.lk.SalesListItem.ACTIVATE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активация оффера (бесплатная)")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ActivateOfferTest {

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

    @Parameterized.Parameter
    public String mockInactive;

    @Parameterized.Parameter(1)
    public String mockActivate;

    @Parameterized.Parameter(2)
    public String mockId;

    @Parameterized.Parameter(3)
    public String category;

    @Parameterized.Parameter(4)
    public String json;

    @Parameterized.Parameters(name = "name = {index}: {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"desktop-lk/UserOffersCarsInactive", "desktop-lk/UserOffersCarsActivate", "desktop-lk/UserOffersCarsId",
                        CARS, "desktop-lk/UserOffersCarsActive"},
                {"desktop-lk/UserOffersMotoInactive", "desktop-lk/UserOffersMotoActivate", "desktop-lk/UserOffersMotoId",
                        MOTO, "desktop-lk/UserOffersMotoActive"},
                {"desktop-lk/UserOffersTrucksInactive", "desktop-lk/UserOffersTrucksActivate",
                        "desktop-lk/UserOffersTrucksId", TRUCKS, "desktop-lk/UserOffersTrucksActive"},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub(mockInactive),
                stub(mockActivate),
                stub(mockId),
                stub().withGetDeepEquals(format("/1.0/user/offers%scount", category))
                        .withRequestQuery(Query.query().setCategory(category.replaceAll("/", "")))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(category).open();

        mockRule.overwriteStub(2, stub(json));
    }

    @Test
    @Owner(KIRILL_PKR)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Активировать»")
    public void shouldClickActivateButton() {
        basePageSteps.onLkSalesPage().getSale(0).hover();
        basePageSteps.onLkSalesPage().getSale(0).button(ACTIVATE).click();

        basePageSteps.onLkSalesPage().getSale(0).chart().should(isDisplayed());
        basePageSteps.onLkSalesPage().getSale(0).vas().should(isDisplayed());
    }
}
