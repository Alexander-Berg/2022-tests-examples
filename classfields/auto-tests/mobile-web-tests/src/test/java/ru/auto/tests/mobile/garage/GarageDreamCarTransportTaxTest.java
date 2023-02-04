package ru.auto.tests.mobile.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mock.MockGarageCard;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static io.restassured.http.Method.PUT;
import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.MATCHES;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.mock.MockGarageCard.CAN_BE_CLARIFIED;
import static ru.auto.tests.desktop.mock.MockGarageCard.DREAM_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.NOT_ENOUGH_DATA;
import static ru.auto.tests.desktop.mock.MockGarageCard.OK;
import static ru.auto.tests.desktop.mock.MockGarageCard.SEDAN;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок транспортного налога на карточке машины мечты")
@Epic(AutoruFeatures.GARAGE)
@Feature(AutoruFeatures.DREAM_CAR)
@Story("Блок транспортного налога")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageDreamCarTransportTaxTest {

    private static final String GARAGE_CARD_ID = "1146321503";
    private static final int TAX = 3900;
    private static final String REGION_NAME = "Санкт-Петербург";
    private static final String NEW_REGION_NAME = "Химки";
    private static final int POWER = 151;
    private static final int NEW_POWER = 122;

    private static final String TRANSPORT_TAX_TEMPLATE = "%s\nТранспортный налог за 2022 год\n%s\nРегион учёта\n" +
            "1.4 AMT (%d л.с.) бензин\nДвигатель";
    private static final String TRANSPORT_TAX_NO_BODY_TEMPLATE = "%s\nТранспортный налог за 2022 год\n" +
            "%s\nРегион учёта\nУкажите тип кузова\n1.4 AMT (%d л.с.) бензин\nДвигатель";
    private static final String TECH_PARAMS_TEMPLATE = "1.4\u00a0AMT (%d\u00a0л.с.) бензин";

    private MockGarageCard garageCard;
    private MockGarageCard garageCardRequest;

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
        garageCard = garageCardOffer()
                .setId(GARAGE_CARD_ID)
                .setCardType(DREAM_CAR)
                .setTaxAmount(TAX)
                .setTaxRegionInfoName(REGION_NAME)
                .setTaxBlockState(CAN_BE_CLARIFIED)
                .setPower(POWER);

        garageCardRequest = garageCardRequest().setCardType(DREAM_CAR);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(garageCard.getBody())
        ).create();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текст блока «Транспортный налог»")
    public void shouldSeeTransportTaxText() {
        basePageSteps.onGarageCardPage().transportTax().should(hasText(
                format(TRANSPORT_TAX_TEMPLATE,
                        formatPrice(TAX), REGION_NAME, POWER)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем регион в блоке «Транспортный налог», проверяем обновленный текст блока")
    public void shouldChangeRegionTransportTax() {
        basePageSteps.onGarageCardPage().transportTax().button(REGION_NAME).click();

        mockRule.setStubs(
                stub("desktop/GeoSuggest"),
                stub().withPath(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withPredicateType(MATCHES)
                        .withMethod(PUT)
                        .withRequestBody(
                                garageCardRequest.setRegistrationRegion(10758, NEW_REGION_NAME).getBody())
                        .withResponseBody(
                                garageCard.getBody())
        ).update();

        mockRule.overwriteStub(1,
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCard.setTaxRegionInfoName(NEW_REGION_NAME).
                                        setTaxBlockState(OK).getBody()));

        basePageSteps.onGarageCardPage().geoSuggestPopup().input().sendKeys(NEW_REGION_NAME);
        basePageSteps.onGarageCardPage().geoSuggestPopup().regionContains(NEW_REGION_NAME).click();

        basePageSteps.onGarageCardPage().transportTax().should(hasText(
                format(TRANSPORT_TAX_TEMPLATE, formatPrice(TAX), NEW_REGION_NAME, POWER)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем технический параметр в блоке «Транспортный налог», проверяем обновленный текст блока")
    public void shouldChangeTechParamTransportTax() {
        String newTechParamId = "7720682";
        String newTechParamName = "1.4 (122\u00a0л.с.), бензин";

        mockRule.setStubs(stub(
                "desktop/ReferenceCatalogCarsSuggestVWJetta")
        ).update();

        basePageSteps.onGarageCardPage().transportTax().button(format(TECH_PARAMS_TEMPLATE, POWER)).click();

        mockRule.setStubs(
                stub().withPath(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withPredicateType(MATCHES)
                        .withMethod(PUT)
                        .withRequestBody(
                                garageCardRequest.setTechParamId(newTechParamId).getBody())
                        .withResponseBody(
                                garageCard.getBody())
        ).update();

        mockRule.overwriteStub(1,
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCard.setPower(NEW_POWER).
                                        setTaxBlockState(OK).getBody()));

        basePageSteps.onGarageCardPage().transportTax().itemInPopup(newTechParamName).click();

        basePageSteps.onGarageCardPage().transportTax().should(hasText(
                format(TRANSPORT_TAX_TEMPLATE,
                        formatPrice(TAX), REGION_NAME, NEW_POWER)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меняем кузов в блоке «Транспортный налог», проверяем обновленный текст блока")
    public void shouldChangeBodyTransportTax() {
        mockRule.overwriteStub(1,
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCard.removeBodyType().getBody()));
        basePageSteps.refresh();

        basePageSteps.onGarageCardPage().transportTax().waitUntil(hasText(
                format(TRANSPORT_TAX_NO_BODY_TEMPLATE,
                        formatPrice(TAX), REGION_NAME, POWER)));

        mockRule.setStubs(stub(
                "desktop/ReferenceCatalogCarsSuggestVWJettaWithoutBody")
        ).update();

        basePageSteps.onGarageCardPage().transportTax().button("Укажите тип кузова").click();

        mockRule.setStubs(
                stub().withPath(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withPredicateType(MATCHES)
                        .withMethod(PUT)
                        .withRequestBody(
                                garageCardRequest.getBody())
                        .withResponseBody(
                                garageCard.getBody())
        ).update();

        mockRule.overwriteStub(1,
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCard.setBodyType(SEDAN).
                                        setTaxBlockState(OK).getBody()));

        basePageSteps.onGarageCardPage().transportTax().itemInPopup("Седан").click();

        basePageSteps.onGarageCardPage().transportTax().should(hasText(
                format(TRANSPORT_TAX_TEMPLATE,
                        formatPrice(TAX), REGION_NAME, POWER)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Проверяем, что блок «Транспортный налог» не отображается при плохих данных")
    public void shouldHideTaxBlock() {
        mockRule.overwriteStub(1,
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCard.setTaxBlockState(NOT_ENOUGH_DATA).getBody()));

        basePageSteps.refresh();

        basePageSteps.onGarageCardPage().transportTax().should(not(isDisplayed()));
    }
}
