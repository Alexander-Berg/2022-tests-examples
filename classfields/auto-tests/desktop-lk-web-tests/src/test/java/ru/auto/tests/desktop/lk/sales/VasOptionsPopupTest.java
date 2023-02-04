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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mock.beans.stub.Query;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_COLORS;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_SPECIAL;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.ALL_SALE_TOPLIST;
import static ru.auto.tests.desktop.consts.SaleServices.VasProduct.PACKAGE_TURBO;
import static ru.auto.tests.desktop.element.lk.Vas.VALID_FOR;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Попапы отдельных опций")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_PRIVATE)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VasOptionsPopupTest {

    public static final String TOPLIST_POPUP = "Поднятие в ТОП\n" +
            "Ваше объявление окажется в специальном блоке на самом верху списка при сортировке по актуальности " +
            "или по дате. Покупатели вас точно не пропустят.";

    public static final String SPECIAL_POPUP = "Спецпредложение\n" +
            "Ваше объявление будет отображаться в специальном блоке в результатах поиска и на карточках объявлений" +
            " о продаже аналогичных авто. А для легковых — также на главной странице и в Каталоге.";

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
        mockRule.setStubs(
                sessionAuthUserStub(),
                stub("desktop/User"),
                stub("desktop-lk/BillingAutoruPaymentInitTurbo"),
                stub("desktop-lk/BillingAutoruPaymentProcess"),
                stub("desktop-lk/BillingAutoruPayment"),
                stub().withGetDeepEquals(userOfferPath).withResponseBody(
                        userOffersResponse().setOffers(
                                mockUserOffer(userOfferPathToMock)
                                        .setServices(
                                                service(PACKAGE_TURBO).setProlongable(true),
                                                service(ALL_SALE_COLORS).setProlongable(false),
                                                service(ALL_SALE_SPECIAL).setProlongable(false),
                                                service(ALL_SALE_TOPLIST).setProlongable(false)
                                        )).build()),
                stub().withGetDeepEquals(userOfferCountPath)
                        .withRequestQuery(Query.query().setCategory(category.replaceAll("/", "")))
                        .withResponseBody(offersCount().getBody())
        ).create();

        urlSteps.testing().path(MY).path(category).open();
        basePageSteps.onLkSalesPage().getSale(0).vas().getTab(2).click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Попап по ховеру «Поднятие в ТОП», нет автопродления")
    public void shouldSeeToplistPopup() {
        basePageSteps.onLkSalesPage().getSale(0).vas().optionDaysLeft("Поднятие в ТОП")
                .should(hasText(startsWith(VALID_FOR))).hover();

        basePageSteps.onLkSalesPage().popup().should(hasText(TOPLIST_POPUP));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Попап по ховеру «Спецпредложение», нет автопродления")
    public void shouldSeeSpecialPopup() {
        basePageSteps.onLkSalesPage().getSale(0).vas().optionDaysLeft("Спецпредложение")
                .should(hasText(startsWith(VALID_FOR))).hover();

        basePageSteps.onLkSalesPage().popup().should(hasText(SPECIAL_POPUP));
    }

}
