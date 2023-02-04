package ru.auto.tests.mobile.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.page.GarageCardPage;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static io.restassured.http.Method.DELETE;
import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.DEEP_EQUALS;
import static ru.auto.tests.desktop.component.WithButton.SAVE;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.element.garage.CardForm.DATE_OF_PURCHASE;
import static ru.auto.tests.desktop.element.garage.CardForm.MILEAGE;
import static ru.auto.tests.desktop.element.garage.CardForm.MONTH;
import static ru.auto.tests.desktop.element.garage.CardForm.YEAR;
import static ru.auto.tests.desktop.mobile.page.GarageCardPage.CHANGE;
import static ru.auto.tests.desktop.mobile.page.GaragePage.ADD_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.EX_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Бывшее авто в гараже")
@Feature(AutoruFeatures.GARAGE)
@Story(AutoruFeatures.EX_CAR)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageExCarTest {

    private static final String GARAGE_CARD_ID = "1146321503";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(garageCardOffer()
                                .setId(GARAGE_CARD_ID)
                                .setCardType(EX_CAR).getBody())
        ).create();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Клик по кнопке «Удалить из гаража» на бывшем авто")
    public void shouldDeleteExCar() {
        mockRule.setStubs(stub("desktop/ReferenceCatalogCarsSuggestVWJetta")).update();
        basePageSteps.onGarageCardPage().button(CHANGE).click();

        mockRule.setStubs(
                stub().withPath(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withPredicateType(DEEP_EQUALS)
                        .withMethod(DELETE)
                        .withStatusSuccessResponse()
        ).update();

        basePageSteps.onGarageCardPage().form().button(GarageCardPage.DELETE).click();
        basePageSteps.acceptAlert();

        basePageSteps.onGaragePage().button(ADD_CAR).should(isDisplayed());
        urlSteps.testing().path(GARAGE).shouldNotSeeDiff();
        basePageSteps.onGarageCardPage().form().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редактируем бывшую машину в гараже")
    public void shouldEditExCar() {
        mockRule.setStubs(stub("desktop/ReferenceCatalogCarsSuggestVWJetta")).update();
        basePageSteps.onGarageCardPage().button(CHANGE).click();

        basePageSteps.onGarageCardPage().form().unfoldedBlock(MILEAGE)
                .input(MILEAGE, "100000");
        basePageSteps.onGarageCardPage().form().unfoldedBlock(DATE_OF_PURCHASE)
                .input(YEAR, "2018");
        basePageSteps.onGarageCardPage().form().unfoldedBlock(DATE_OF_PURCHASE)
                .input(MONTH, "2");

        mockRule.setStubs(stub("desktop/GarageUserCardPutVWJetta")).update();
        basePageSteps.onGarageCardPage().form().button(SAVE).click();

        basePageSteps.onGarageCardPage().form().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

}
