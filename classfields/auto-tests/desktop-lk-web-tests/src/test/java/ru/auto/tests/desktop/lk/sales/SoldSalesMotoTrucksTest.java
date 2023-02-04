package ru.auto.tests.desktop.lk.sales;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.KIRILL_PKR;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SCOOTERS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.lk.SalesListItem.DELETE;
import static ru.auto.tests.desktop.element.lk.SalesListItem.LK_EDIT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO_COUNT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS_COUNT;
import static ru.auto.tests.desktop.page.lk.LkSalesPage.OFFER_DELETED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Снятое с продажи объявление в Мото и Комтрансе")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SoldSalesMotoTrucksTest {

    private final static String SALE_ID = "1076842087-f1e84";
    private final static String MOTO_URL = "/vespa/et_4/";
    private final static String TRUCKS_URL = "/ford/courier/";

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
    private ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String mockInactive;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameter(2)
    public String mockDelete;

    @Parameterized.Parameter(3)
    public String subcategory;

    @Parameterized.Parameter(4)
    public String offerUrl;

    @Parameterized.Parameter(5)
    public String userOfferCountPath;

    @Parameterized.Parameters(name = "name = {index}: {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"desktop-lk/UserOffersMotoInactive", MOTO, "desktop-lk/UserOffersMotoDelete", SCOOTERS,
                        MOTO_URL, USER_OFFERS_MOTO_COUNT},
                {"desktop-lk/UserOffersTrucksInactive", TRUCKS, "desktop-lk/UserOffersTrucksDelete", LCV,
                        TRUCKS_URL, USER_OFFERS_TRUCKS_COUNT},
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub(mockInactive),
                stub().withGetDeepEquals(userOfferCountPath)
                        .withRequestQuery(Query.query().setCategory(category.replaceAll("/", "")))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(category).open();
    }

    @Test
    @Owner(KIRILL_PKR)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Отображение снятого с продажи объявления")
    public void shouldSeeSoldSale() {
        screenshotSteps.setWindowSizeForScreenshot();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().getSale(0));

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onLkSalesPage().getSale(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(KIRILL_PKR)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по заголовку объявления")
    public void shouldClickSaleTitle() {
        basePageSteps.onLkSalesPage().getSale(0).title().hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(subcategory).path(USED).path(SALE).path(offerUrl).path(SALE_ID).path("/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(KIRILL_PKR)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onLkSalesPage().getSale(0).hover();
        basePageSteps.onLkSalesPage().getSale(0).button(LK_EDIT).waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(category).path(EDIT).path(SALE_ID).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(KIRILL_PKR)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Удалить»")
    public void shouldClickDeleteButton() {
        mockRule.setStubs(stub(mockDelete)).update();

        basePageSteps.onLkSalesPage().getSale(0).hover();
        basePageSteps.onLkSalesPage().getSale(0).button(DELETE).waitUntil(isDisplayed()).click();
        basePageSteps.acceptAlert();
        basePageSteps.onLkSalesPage().notifier().waitUntil(isDisplayed()).should(hasText(OFFER_DELETED));

        basePageSteps.onLkSalesPage().salesList().should(hasSize(0));
    }

}
