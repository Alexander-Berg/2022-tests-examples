package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.consts.QueryParams;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import static java.lang.String.format;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DKP;
import static ru.auto.tests.desktop.consts.Pages.DOCS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PRINT;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOfferStats.getCounter;
import static ru.auto.tests.desktop.mock.MockUserOfferStats.getDateDaysAgo;
import static ru.auto.tests.desktop.mock.MockUserOfferStats.userOfferStats;
import static ru.auto.tests.desktop.mock.MockUserOfferStats.userOfferStatsForLastDays;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.mock.beans.userOfferStats.StatsItem.stats;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Активное развёрнутое объявление")
@Epic(AutoruFeatures.LK_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
public class ActiveUnfoldedSaleCarsTest {

    private final static String SALE_ID = "1076842087";
    private final static String SALE_ID_HASH = "1076842087-f1e84";
    private final static String MARK = "vaz";
    private final static String MODEL = "2121";
    private final static int VIEWS_COUNT = 12;
    private final static int CALLS_COUNT = 4;
    private final static int FAVORITES_COUNT = 6;

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

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS_COUNT).withRequestQuery(Query.query()
                                .setCategory(AutoApiOffer.CategoryEnum.CARS.getValue()))
                        .withResponseBody(offersCount().getBody()),

                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/UserOffersCarsActive"),
                stub("desktop-lk/UserOffersCarsId"),
                stub().withGetDeepEquals(format("%s/%s/stats", USER_OFFERS_CARS, SALE_ID_HASH))
                        .withResponseBody(userOfferStatsForLastDays(SALE_ID_HASH).getBody())
        ).create();

        urlSteps.testing().path(MY).path(CARS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по заголовку объявления»")
    public void shouldClickSaleTitle() {
        basePageSteps.onLkSalesNewPage().getSale(0).title().click();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(MARK).path(MODEL)
                .path(SALE_ID_HASH).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).editButton().should(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(CARS).path(USED).path(EDIT).path(SALE_ID_HASH).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Договор»")
    public void shouldClickDocButton() {
        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).dotsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().threeDotsMenu().moreMenuButton("Договор купли-продажи").waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(DOCS).path(DKP).addParam(QueryParams.SALE_ID, SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Owner(DENISKOROBOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Лист продажи»")
    public void shouldClickPrintButton() {
        basePageSteps.onLkSalesNewPage().getSale(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).dotsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onLkSalesNewPage().threeDotsMenu().moreMenuButton("Лист продажи").waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();

        urlSteps.testing().path(MY).path(CARS).path(PRINT).path(SALE_ID_HASH).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ховер по столбику на графике")
    public void shouldHoverChartItem() {
        mockRule.overwriteStub(4,
                stub().withGetDeepEquals(format("%s/%s/stats", USER_OFFERS_CARS, SALE_ID_HASH))
                        .withResponseBody(userOfferStats().setStats(
                                stats().setOfferId(SALE_ID_HASH)
                                        .setCounters(asList(
                                                getCounter().setDate(getDateDaysAgo(0))
                                                        .setViews(VIEWS_COUNT)
                                                        .setFavoriteTotal(FAVORITES_COUNT)
                                                        .setPhoneCalls(CALLS_COUNT)))).getBody()));
        basePageSteps.refresh();

        basePageSteps.onLkSalesNewPage().getSale(0).chart().hover();
        basePageSteps.onLkSalesNewPage().getSale(0).chart().notEmptyColumnsList().get(0).hover();

        basePageSteps.onLkSalesNewPage().activePopup()
                .should(hasText(containsString(format("%d звонка\n%d сохранили\n%d просмотров",
                        CALLS_COUNT, FAVORITES_COUNT, VIEWS_COUNT))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Добавить панораму»")
    public void shouldClickAddPanoramaButton() {
        basePageSteps.onLkSalesNewPage().getSale(0).addPanoramaButton().click();

        basePageSteps.onLkSalesNewPage().popup().waitUntil(isDisplayed()).should(hasText("360°\nС панорамой — " +
                "больше звонков до 2,5 раз\nСнять панораму можно в приложении Авто.ру на любом смартфоне. " +
                "Отсканируйте QR-код, чтобы скачать приложение."));
    }

}
