package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static io.restassured.http.Method.DELETE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.DEEP_EQUALS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.AVGRIBANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mobile.component.WithButton.DELETE_TEXT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("ЛК - удаление объявления")
@Epic(LK)
@Feature(SALES)
@Story("Удаление объявления")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DeleteOfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String mockInactive;

    @Parameterized.Parameter(1)
    public String mockEmpty;

    @Parameterized.Parameter(2)
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"desktop/UserOffersCarsInactive", "desktop/UserOffersCarsEmpty", CARS},
                {"desktop/UserOffersMotoInactive", "desktop/UserOffersMotoEmpty", MOTO},
                {"desktop/UserOffersTrucksInactive", "desktop/UserOffersTrucksEmpty", TRUCKS},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub(mockInactive),
                stub().withPredicateType(DEEP_EQUALS)
                        .withPath(format("/1.0/user/offers%s1076842087-f1e84", category))
                        .withMethod(DELETE)
                        .withStatusSuccessResponse()).create();

        urlSteps.testing().path(MY).path(category).open();

        mockRule.overwriteStub(1, stub(mockEmpty));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Клик по кнопке «Удалить»")
    public void shouldClickDeleteButton() {
        basePageSteps.onLkPage().getSale(0).button(DELETE_TEXT).click();
        basePageSteps.acceptAlert();
        basePageSteps.onLkPage().salesList().waitUntil(hasSize(0));

        basePageSteps.onLkPage().stub().should(isDisplayed());
    }
}
