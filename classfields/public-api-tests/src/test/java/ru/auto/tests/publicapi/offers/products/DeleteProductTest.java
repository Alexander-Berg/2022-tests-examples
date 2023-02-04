package ru.auto.tests.publicapi.offers.products;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("DELETE /user/offers/{category}/{offerID}/products")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiDealerModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DeleteProductTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiDealerAdaptor adaptor;

    @Inject
    private Account account;

    @Parameter("Категория")
    @Parameterized.Parameter
    public CategoryEnum category;

    @Parameter("Создаваемый оффер")
    @Parameterized.Parameter(1)
    public String offerPath;

    @Parameter("Услуга")
    @Parameterized.Parameter(2)
    public String code;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {CARS, "offers/dealer_new_cars.json", "all_sale_special"},
                {CARS, "offers/dealer_new_cars.json", "all_sale_premium"},
                {MOTO, "offers/dealer_moto.json", "all_sale_special"},
                {MOTO, "offers/dealer_moto.json", "all_sale_premium"},
                {TRUCKS, "offers/dealer_trucks.json", "all_sale_special"},
                {TRUCKS, "offers/dealer_trucks.json", "all_sale_premium"}
        });
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldDeleteProduct() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, category, offerPath).getOfferId();
        adaptor.applyProduct(code, category, offerId, sessionId);

        api.userOffers().deleteProducts().categoryPath(category).offerIDPath(offerId).productQuery(code)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }
}
