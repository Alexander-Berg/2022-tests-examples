package ru.auto.tests.mobile.electro;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.Notifications.ADDED_TO_GARAGE;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.ELECTRO;
import static ru.auto.tests.desktop.consts.Pages.ENGINE_ELECTRO;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ELECTROPAGE;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.mock.MockCatalogEntity.TESLA_MODEL_3;
import static ru.auto.tests.desktop.mock.MockCatalogEntity.mockCatalogEntity;
import static ru.auto.tests.desktop.mock.MockGarageCard.DREAM_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.TESLA_MODEL_3_GARAGE_CARD;
import static ru.auto.tests.desktop.mock.MockGarageCard.TESLA_MODEL_3_REQUEST_TO_DREAM_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCard;
import static ru.auto.tests.desktop.mock.MockGarageCards.garageCards;
import static ru.auto.tests.desktop.mock.MockGarageCards.getGarageCardsRequest;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockSearchCars.SEARCH_OFFERS_TESLA_MODEL_3;
import static ru.auto.tests.desktop.mock.MockSearchCars.getSearchOffersQuery;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffers;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARDS;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.mock.Paths.REFERENCE_CATALOG_CARS;
import static ru.auto.tests.desktop.mock.Paths.SEARCH_CARS;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.page.ElectroModelPage.GO_TO_GARAGE;
import static ru.auto.tests.desktop.page.ElectroModelPage.PUT_INTO_GARAGE;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(AutoruFeatures.ELECTRO)
@Feature("Страница модели")
@DisplayName("Страница модели")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ElectroModelTest {

    private static final String GARAGE_CARD_ID = "1146324503";
    private static final String CATALOG_CARD_IDS = "21057314_21377601_23069875";
    private static final String SALE_ID = "1114782187-3302e085";

    private static final String TECH_PARAM_ID = "23069875";

    private static final String MARK_NAME = "Марка";
    private static final String MODEL_NAME = "Модель";
    private static final String TECH_PARAM_NAME = "Тех. параметры";
    private static final String SUPER_GEN_NAME = "Поколение";
    private static final int MILEAGE = 234;

    private static final int ELECTRIC_RANGE = 569;
    private static final int CHARGE_TIME = 22;
    private static final int POWER = 290;
    private static final int POWER_KVT = 172;
    private static final double ACCELERATION = 4.1;

    private static final int PRICE = 6600300;
    private static final int YEAR = 2018;
    private static final String REGION_NAME = "Петрозаводск";

    private static final String TESLA = "tesla";
    private static final String MODEL_3 = "model_3";

    private static final String LANDING_DESCRIPTION = "Электрический седан D-класса, доступный с двумя моторами. " +
            "Мощность, в зависимости от версии, варьируется от 258 до 462 л.с., запас хода достигает 354 км. " +
            "Самая быстрая версия разгоняется до 100 км/ч за 3,5 секунды.";

    private Query searchOffersQuery = getSearchOffersQuery()
            .setCatalogFilter("mark=TESLA,model=MODEL_3")
            .setEngineGroup("ELECTRO")
            .setPageSize("6");

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(ELECTRO).path(CARS).path(TESLA).path(MODEL_3).path("21057266-21057314")
                .path(TECH_PARAM_ID);
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Название модели")
    public void shouldSeeTitle() {
        mockRule.setStubs(
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS)
                        .withRequestQuery(
                                query().setTechParamId(TECH_PARAM_ID))
                        .withResponseBody(
                                mockCatalogEntity(TESLA_MODEL_3)
                                        .setMarkName(MARK_NAME)
                                        .setModelName(MODEL_NAME)
                                        .setTechParamName(TECH_PARAM_NAME)
                                        .setSuperGenName(SUPER_GEN_NAME).build())
        ).create();

        urlSteps.open();

        basePageSteps.onElectroModelPage().title().should(hasText(format("%s %s %s %s",
                MARK_NAME, MODEL_NAME, TECH_PARAM_NAME, SUPER_GEN_NAME)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Описание модели")
    public void shouldSeeDescription() {
        mockRule.setStubs(
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS)
                        .withRequestQuery(
                                query().setTechParamId(TECH_PARAM_ID))
                        .withResponseBody(
                                mockCatalogEntity(TESLA_MODEL_3)
                                        .setLandingDescription(LANDING_DESCRIPTION).build())
        ).create();

        urlSteps.open();

        basePageSteps.onElectroModelPage().description().should(hasText(LANDING_DESCRIPTION));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Табличка тех. характеристик")
    public void shouldSeeTechParams() {
        mockRule.setStubs(
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS)
                        .withRequestQuery(
                                query().setTechParamId(TECH_PARAM_ID))
                        .withResponseBody(
                                mockCatalogEntity(TESLA_MODEL_3)
                                        .setElectricRange(ELECTRIC_RANGE)
                                        .setChargeTime(CHARGE_TIME)
                                        .setPowerKvt(POWER_KVT)
                                        .setPower(POWER)
                                        .setAcceleration(ACCELERATION).build())
        ).create();

        urlSteps.open();

        basePageSteps.onElectroModelPage().techParamsTable().should(hasText(format(
                "Запас хода на электротяге\nВремя зарядки\n%d км %d ч\nМощность\nРазгон\n%d кВт (%d л.с.) %.1f с",
                ELECTRIC_RANGE, CHARGE_TIME, POWER_KVT, POWER, ACCELERATION)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавляем модель в гараж")
    public void shouldAddModelToGarage() {
        mockRule.setStubs(
                sessionAuthUserStub(),
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS)
                        .withRequestQuery(
                                query().setTechParamId(TECH_PARAM_ID))
                        .withResponseBody(
                                mockCatalogEntity(TESLA_MODEL_3).build())
        ).create();

        urlSteps.open();

        mockRule.setStubs(
                stub().withPostDeepEquals(GARAGE_USER_CARD)
                        .withRequestBody(
                                garageCard(TESLA_MODEL_3_REQUEST_TO_DREAM_CAR).getBody())
                        .withResponseBody(
                                garageCard(TESLA_MODEL_3_GARAGE_CARD).getBody()),
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withRequestBody(
                                getGarageCardsRequest())
                        .withResponseBody(
                                garageCards().setCards(garageCard(TESLA_MODEL_3_GARAGE_CARD)).build())
        ).update();

        basePageSteps.onElectroModelPage().toGarage().waitUntil(hasText(PUT_INTO_GARAGE)).click();

        basePageSteps.onElectroModelPage().notifier(ADDED_TO_GARAGE).should(isDisplayed());
        basePageSteps.onElectroModelPage().toGarage().should(hasText(GO_TO_GARAGE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переходим в гараж")
    public void shouldGoToGarage() {
        mockRule.setStubs(
                sessionAuthUserStub(),
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS)
                        .withRequestQuery(
                                query().setTechParamId(TECH_PARAM_ID))
                        .withResponseBody(
                                mockCatalogEntity(TESLA_MODEL_3).build()),
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withRequestBody(
                                getGarageCardsRequest())
                        .withResponseBody(
                                garageCards().setCards(
                                        garageCard(TESLA_MODEL_3_GARAGE_CARD).setId(GARAGE_CARD_ID)).build())
        ).create();

        urlSteps.open();

        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCard(TESLA_MODEL_3_GARAGE_CARD)
                                        .setId(GARAGE_CARD_ID)
                                        .setCardType(DREAM_CAR).getBody())
        ).update();

        basePageSteps.onElectroModelPage().toGarage().waitUntil(hasText(GO_TO_GARAGE)).click();

        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).path(SLASH).shouldNotSeeDiff();

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заголовок над блоком офферов в продаже")
    public void shouldSeeOffersListTitle() {
        mockRule.setStubs(
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS)
                        .withRequestQuery(
                                query().setTechParamId(TECH_PARAM_ID))
                        .withResponseBody(
                                mockCatalogEntity(TESLA_MODEL_3)
                                        .setMarkName(MARK_NAME)
                                        .setModelName(MODEL_NAME).build()),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                searchOffersQuery)
                        .withResponseBody(
                                searchOffers(SEARCH_OFFERS_TESLA_MODEL_3).getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onElectroModelPage().h2().should(hasText(format("Электромобили %s %s в продаже",
                MARK_NAME, MODEL_NAME)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сниппета объявления")
    public void shouldSeeCarOffer() {
        mockRule.setStubs(
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS)
                        .withRequestQuery(
                                query().setTechParamId(TECH_PARAM_ID))
                        .withResponseBody(
                                mockCatalogEntity(TESLA_MODEL_3).build()),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                searchOffersQuery)
                        .withResponseBody(
                                searchOffers(SEARCH_OFFERS_TESLA_MODEL_3)
                                        .setRurPrice(PRICE)
                                        .setMarkName(MARK_NAME)
                                        .setModelName(MODEL_NAME)
                                        .setTechparamNameplate(TECH_PARAM_NAME)
                                        .setSuperGenName(SUPER_GEN_NAME)
                                        .setYear(YEAR)
                                        .setMileage(MILEAGE)
                                        .setRegionName(REGION_NAME).getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onElectroModelPage().offers().waitUntil(hasSize(8)).get(0).should(
                hasText(format("%s\n%s\n%s %s %s %s\n%d г., %d км",
                        REGION_NAME, formatPrice(PRICE), MARK_NAME, MODEL_NAME, TECH_PARAM_NAME,
                        SUPER_GEN_NAME, YEAR, MILEAGE)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход по сниппету объявления")
    public void shouldGoToCarOffer() {
        mockRule.setStubs(
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS)
                        .withRequestQuery(
                                query().setTechParamId(TECH_PARAM_ID))
                        .withResponseBody(
                                mockCatalogEntity(TESLA_MODEL_3).build()),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                searchOffersQuery)
                        .withResponseBody(
                                searchOffers(SEARCH_OFFERS_TESLA_MODEL_3).setId(SALE_ID).getBody()),
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(mockOffer(CAR_EXAMPLE).setId(SALE_ID).getResponse())
        ).create();

        urlSteps.open();
        basePageSteps.onElectroModelPage().offers().get(0).click();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("honda").path("shuttle").path(SALE_ID).path(SLASH)
                .addParam(FROM, ELECTROPAGE).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход ко всем объявлениям")
    public void shouldGoToAllCarOffers() {
        int offersCount = getRandomBetween(10, 99);

        mockRule.setStubs(
                stub().withGetDeepEquals(REFERENCE_CATALOG_CARS)
                        .withRequestQuery(
                                query().setTechParamId(TECH_PARAM_ID))
                        .withResponseBody(
                                mockCatalogEntity(TESLA_MODEL_3).build()),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                searchOffersQuery)
                        .withResponseBody(
                                searchOffers(SEARCH_OFFERS_TESLA_MODEL_3).setTotalOffersCount(offersCount).getBody()),
                stub("desktop/SearchCarsBreadcrumbsEmpty")
        ).create();

        urlSteps.open();

        basePageSteps.onElectroModelPage().showAllOffers().waitUntil(hasText(matchesPattern(
                format("Смотреть все %d предложени[яйе]", offersCount)))).hover().click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(TESLA).path(MODEL_3).path(ALL).path(ENGINE_ELECTRO)
                .path(SLASH).shouldNotSeeDiff();
    }

}
