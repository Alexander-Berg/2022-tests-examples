package ru.auto.tests.desktop.lk.salesNew;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.Notifications.AUTOPROLONG_ACTIVATED;
import static ru.auto.tests.desktop.consts.Notifications.AUTOPROLONG_DEACTIVATED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.PACKAGE_TURBO;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_CAR_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_MOTO_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.USER_OFFER_TRUCK_EXAMPLE;
import static ru.auto.tests.desktop.mock.MockUserOffer.mockUserOffer;
import static ru.auto.tests.desktop.mock.MockUserOffer.service;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.MockUserOffersCount.offersCount;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS_COUNT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_MOTO_COUNT;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_TRUCKS_COUNT;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_19219;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Автопродление уже подключенного пакета")
@Epic(AutoruFeatures.LK_NEW)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Ignore
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasAutoprolongActivePackageTest {

    private static final String ID_PACKAGE_TURBO = "/1076842087-f1e84/product/package_turbo/prolongable";

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
                {CARS, USER_OFFERS_CARS, USER_OFFER_CAR_EXAMPLE, USER_OFFERS_CARS_COUNT},
                {TRUCKS, USER_OFFERS_TRUCKS, USER_OFFER_TRUCK_EXAMPLE, USER_OFFERS_TRUCKS_COUNT},
                {MOTO, USER_OFFERS_MOTO, USER_OFFER_MOTO_EXAMPLE, USER_OFFERS_MOTO_COUNT}
        });
    }

    @Before
    public void before() {
        cookieSteps.setExpFlags(EXP_AUTORUFRONT_19219);

        mockRule.setStubs(stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub().withGetDeepEquals(userOfferPath).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(userOfferPathToMock)
                                        .addProlongationAllowedForServicePrice(PACKAGE_TURBO)
                                        .setServices(service(PACKAGE_TURBO))
                        ).build()),
                stub().withGetDeepEquals(userOfferCountPath)
                        .withRequestQuery(Query.query().setCategory(category.replaceAll("/", "")))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(category).open();
    }


    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Автопродление уже подключенного пакета «Турбо-продажа»")
    public void shouldBuyVas() {
        basePageSteps.onLkSalesNewPage().getSale(0).getVas(0).hover();
        basePageSteps.onLkSalesNewPage().getSale(0).openPopup().waitUntil(isDisplayed());

        mockRule.setStubs(
                stub().withPutDeepEquals(format("%s%s", userOfferPath, ID_PACKAGE_TURBO))
                        .withRequestQuery(query().setDomain("autoru"))
                        .withStatusSuccessResponse()
        ).update();

        basePageSteps.onLkSalesNewPage().getSale(0).openPopup().turnOnButton().click();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText(AUTOPROLONG_ACTIVATED));
        basePageSteps.onLkSalesNewPage().getSale(0).getVas(0).should(hasText("×20 просмотров\nАвтопродление"));

        mockRule.setStubs(
                stub().withDeleteDeepEquals(format("%s%s", userOfferPath, ID_PACKAGE_TURBO))
                        .withRequestQuery(query().setDomain("autoru"))
                        .withStatusSuccessResponse()
        ).update();

        basePageSteps.onLkSalesNewPage().getSale(0).openPopup().waitUntil(isDisplayed());
        basePageSteps.onLkSalesNewPage().getSale(0).openPopup().turnOffButton().click();
        basePageSteps.onLkSalesNewPage().notifier().waitUntil(isDisplayed())
                .waitUntil(hasText(AUTOPROLONG_DEACTIVATED));

        basePageSteps.onLkSalesNewPage().getSale(0).getVas(0)
                .should(hasText(matchesPattern("×20 просмотров\nАктивно ещё\\d+ (день|дня|дней)")));
    }

}
