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
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.ELECTRO;
import static ru.auto.tests.desktop.consts.Pages.ELECTROCARS;
import static ru.auto.tests.desktop.consts.Pages.ENGINE_ELECTRO;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.TAG;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.AUTO_RU;
import static ru.auto.tests.desktop.consts.QueryParams.CPM;
import static ru.auto.tests.desktop.consts.QueryParams.ELECTROPAGE;
import static ru.auto.tests.desktop.consts.QueryParams.ELECTROPAGE_ARTICLES;
import static ru.auto.tests.desktop.consts.QueryParams.ELECTROPAGE_UCHEBNIK;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.consts.QueryParams.UTM_CAMPAIGN;
import static ru.auto.tests.desktop.consts.QueryParams.UTM_CONTENT;
import static ru.auto.tests.desktop.consts.QueryParams.UTM_MEDIUM;
import static ru.auto.tests.desktop.consts.QueryParams.UTM_SOURCE;
import static ru.auto.tests.desktop.element.electro.JournalSection.SHOW_MORE_MATERIALS;
import static ru.auto.tests.desktop.element.electro.ReviewSection.SHOW_MORE_REVIEW;
import static ru.auto.tests.desktop.mock.MockOffer.CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockOffer.mockOffer;
import static ru.auto.tests.desktop.mock.MockReviews.reviewsExample;
import static ru.auto.tests.desktop.mock.MockSearchCars.SEARCH_OFFERS_TESLA_MODEL_3;
import static ru.auto.tests.desktop.mock.MockSearchCars.SEARCH_PROMO_ELECTRO_CARS;
import static ru.auto.tests.desktop.mock.MockSearchCars.getSearchOffersQuery;
import static ru.auto.tests.desktop.mock.MockSearchCars.getSearchPromoElectroQuery;
import static ru.auto.tests.desktop.mock.MockSearchCars.searchOffers;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.OFFER_CARS;
import static ru.auto.tests.desktop.mock.Paths.REVIEWS_AUTO_LISTING;
import static ru.auto.tests.desktop.mock.Paths.SEARCH_CARS;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.utils.Utils.formatPrice;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(AutoruFeatures.ELECTRO)
@Feature("Страница «Электромобили»")
@DisplayName("Страница «Электромобили»")
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ElectroPageTest {

    private static final String MARK_NAME = "Марка";
    private static final String MODEL_NAME = "Модель";
    private static final String TECH_PARAM_NAME = "Тех. параметры";
    private static final String SUPER_GEN_NAME = "Поколение";
    private static final int MILEAGE = 234;
    private static final int PRICE = 6600300;
    private static final int YEAR = 2018;
    private static final String REGION_NAME = "Петрозаводск";
    private static final String SALE_ID = "1114782187-3302e085";

    private Query searchOffersQuery = getSearchOffersQuery()
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
        mockRule.setStubs(
                sessionAuthUserStub(),
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("desktop/SearchCarsMarkModelFiltersElectro"),
                stub("desktop/PostsElectro"),
                stub().withGetDeepEquals(REVIEWS_AUTO_LISTING)
                        .withRequestQuery(
                                query().setCategory("CARS").setEngineType("ELECTRO").setPhoto(true).setPageSize("12"))
                        .withResponseBody(
                                reviewsExample().getBody()),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                searchOffersQuery)
                        .withResponseBody(
                                searchOffers(SEARCH_OFFERS_TESLA_MODEL_3).setId(SALE_ID).getBody()),
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                getSearchPromoElectroQuery().setPageSize("24"))
                        .withResponseBody(
                                searchOffers(SEARCH_PROMO_ELECTRO_CARS).getBody())
        ).create();

        urlSteps.testing().path(ELECTRO).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход по посту")
    public void shouldGoToPost() {
        String postPath = "vilka-na-kolyosah-6faktov-opervom-elektricheskom-mini";

        basePageSteps.onElectroPage().postsSection().posts().waitUntil(hasSize(7)).get(0).click();
        basePageSteps.switchToNextTab();

        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(postPath).path(SLASH)
                .addParam(UTM_CONTENT, postPath)
                .addParam(UTM_CAMPAIGN, ELECTROPAGE_UCHEBNIK)
                .addParam(UTM_SOURCE, AUTO_RU)
                .addParam(UTM_MEDIUM, CPM)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход по популярной модели")
    public void shouldGoToPopularModel() {
        basePageSteps.onElectroPage().popularModelsSection().models().waitUntil(hasSize(4)).get(0)
                .waitUntil(hasText("Audi e-tron 55 I")).click();

        urlSteps.testing().path(ELECTRO).path(CARS).path("audi").path("e_tron").path("21447469-21447519")
                .path("22291114").path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход по статье в журнале")
    public void shouldGoToJournalPost() {
        String postTilte = "Volkswagen разместил в Дублине экологичный билборд, нарисованный вручную карандашом";
        String postPath = "volkswagen-razmestil-v-dubline-bilbord-narisovannyy-vruchnuyu-karandashom";

        basePageSteps.onElectroPage().journalSection().posts().waitUntil(hasSize(5)).get(0)
                .waitUntil(hasText(postTilte)).click();
        basePageSteps.switchToNextTab();

        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(postPath).path(SLASH)
                .addParam(UTM_CONTENT, postPath)
                .addParam(UTM_CAMPAIGN, ELECTROPAGE_ARTICLES)
                .addParam(UTM_SOURCE, AUTO_RU)
                .addParam(UTM_MEDIUM, CPM)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход по «Показать больше материалов» в журнале")
    public void shouldGoToJournalMore() {
        basePageSteps.onElectroPage().journalSection().button().waitUntil(hasText(SHOW_MORE_MATERIALS)).click();

        urlSteps.subdomain(SUBDOMAIN_MAG).path(TAG).path(ELECTROCARS).path(SLASH)
                .addParam(UTM_CAMPAIGN, ELECTROPAGE_ARTICLES)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение сниппета объявления")
    public void shouldSeeCarOffer() {
        mockRule.overwriteStub(5,
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
                                        .setRegionName(REGION_NAME).getBody()));

        urlSteps.refresh();

        basePageSteps.onElectroPage().offersSection().offers().waitUntil(hasSize(8)).get(0).should(
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
                stub().withGetDeepEquals(format("%s/%s", OFFER_CARS, SALE_ID))
                        .withResponseBody(mockOffer(CAR_EXAMPLE).setId(SALE_ID).getResponse())
        ).update();

        urlSteps.refresh();
        basePageSteps.onElectroPage().offersSection().offers().get(0).click();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("honda").path("shuttle").path(SALE_ID).path(SLASH)
                .addParam(FROM, ELECTROPAGE).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход ко всем объявлениям")
    public void shouldGoToAllCarOffers() {
        int offersCount = getRandomBetween(10, 99);

        mockRule.overwriteStub(5,
                stub().withGetDeepEquals(SEARCH_CARS)
                        .withRequestQuery(
                                searchOffersQuery)
                        .withResponseBody(
                                searchOffers(SEARCH_OFFERS_TESLA_MODEL_3)
                                        .setTotalOffersCount(offersCount)
                                        .setEmptyCatalogFilterSearchParameters().getBody())
        );

        urlSteps.refresh();

        basePageSteps.onElectroPage().offersSection().button().waitUntil(hasText(matchesPattern(
                format("Смотреть все %d предложени[яйе]", offersCount)))).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).path(ENGINE_ELECTRO).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход по отзыву")
    public void shouldGoToReview() {
        String reviewTitle = "4,6\nNissan Leaf Electro AT (80.0 кВт)\nИдеальная машина для москвы";

        basePageSteps.onElectroPage().reviewsSection().reviews().waitUntil(hasSize(4)).get(0)
                .waitUntil(hasText(reviewTitle)).click();

        urlSteps.testing().path(REVIEW).path(CARS).path("nissan").path("leaf").path("20349043")
                .path("4917381448292195829").path(SLASH).addParam(FROM, ELECTROPAGE).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход по «Показать больше отзывов»")
    public void shouldGoToReviewMore() {
        basePageSteps.onElectroPage().reviewsSection().button().waitUntil(hasText(SHOW_MORE_REVIEW)).hover().click();

        urlSteps.testing().path(REVIEWS).path(CARS).path(ALL).path(SLASH)
                .addParam("engine_type", "ELECTRO").shouldNotSeeDiff();
    }

}
