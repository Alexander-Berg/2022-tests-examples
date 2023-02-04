package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static io.restassured.http.Method.DELETE;
import static io.restassured.http.Method.PUT;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.DEEP_EQUALS;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.MATCHES;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_FRESH;
import static ru.auto.tests.desktop.mock.MockBillingScheduleRequest.getBillingScheduleBoostBody;
import static ru.auto.tests.desktop.mock.MockBillingScheduleRequest.getBillingScheduleBoostQuery;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_MOTO_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_TRUCK_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffer.service;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.BILLING_SCHEDULES;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO_COUNT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS_COUNT;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Автоподнятие")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasBuyAutoFreshTest {

    private static final String ID = "1076842087-f1e84";
    private static final String PATH_TEMPLATE = format("%s/%s/%s/boost", BILLING_SCHEDULES, "%s", ID);

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String userOfferPath;

    @Parameterized.Parameter(2)
    public String userOfferPathToMock;

    @Parameterized.Parameter(3)
    public String userOfferCountPath;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"cars", USER_OFFERS_CARS, USER_OFFER_CAR_EXAMPLE, USER_OFFERS_CARS_COUNT},
                {"trucks", USER_OFFERS_TRUCKS, USER_OFFER_TRUCK_EXAMPLE, USER_OFFERS_TRUCKS_COUNT},
                {"moto", USER_OFFERS_MOTO, USER_OFFER_MOTO_EXAMPLE, USER_OFFERS_MOTO_COUNT}
        });
    }

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        mockRule.setStubs(
                stub().withGetDeepEquals(userOfferPath).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(userOfferPathToMock)
                                        .setServices(
                                                service(ALL_SALE_FRESH))
                        ).build()),
                stub().withPredicateType(MATCHES)
                        .withMethod(PUT)
                        .withPath(format(PATH_TEMPLATE, category))
                        .withRequestQuery(getBillingScheduleBoostQuery().setOfferId(ID))
                        .withStatusSuccessResponse(),
                stub().withPredicateType(MATCHES)
                        .withMethod(PUT)
                        .withPath(format(PATH_TEMPLATE, category))
                        .withRequestBody(getBillingScheduleBoostBody())
                        .withStatusSuccessResponse(),
                stub().withPredicateType(DEEP_EQUALS)
                        .withMethod(DELETE)
                        .withPath(format(PATH_TEMPLATE, category))
                        .withStatusSuccessResponse(),
                stub().withGetDeepEquals(userOfferCountPath)
                        .withRequestQuery(Query.query().setCategory(category))
                        .withResponseBody(offersCount().getBody()),
                stub("desktop/SessionAuthUser"),
                stub("desktop/UserWithTiedCard")
        ).create();

        urlSteps.testing().path(MY).path(category).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подключаем и отключаем автоподнятие по текущему времени")
    public void shouldTurnAutoFreshOnAndOff() {
        basePageSteps.onLkSalesNewPage().getSale(0).getVas(4).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).openPopup().waitUntil(isDisplayed());

        turnAutoFreshOn();
        turnAutoFreshOff();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подключаем и отключаем автоподнятие по выбранному времени")
    public void shouldSelectTime() {
        basePageSteps.onLkSalesNewPage().getSale(0).getVas(4).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).openPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().getSale(0).openPopup()
                .selectItem("Каждый день в 12:00", "Каждый день в 16:00");

        turnAutoFreshOff();
        turnAutoFreshOn();
    }

    @Step("Включаем автоподнятие")
    private void turnAutoFreshOn() {
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        basePageSteps.onLkSalesNewPage().getSale(0).openPopup().turnOnButton().click();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText("Автоподнятие активировано"));
        basePageSteps.onLkSalesNewPage().getSale(0).getVas(4).should(hasText("×3 просмотров\nАвтопродление"));
    }

    @Step("Выключаем автоподнятие")
    private void turnAutoFreshOff() {
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        basePageSteps.onLkSalesNewPage().getSale(0).openPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().getSale(0).openPopup().turnOffButton().click();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText("Автоподнятие отключено"));
        basePageSteps.onLkSalesNewPage().getSale(0).getVas(4).should(hasText("×3 просмотров\nПодключено в 00:00"));
    }

}
