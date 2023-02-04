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
import ru.auto.tests.publicapi.model.AutoApiApplyAutoruProductsRequest;
import ru.auto.tests.publicapi.model.AutoSalesmanAutoruProduct;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 26.12.18
 */

@DisplayName("POST /user/offers/{category}/{offerID}/products")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiDealerModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ApplyProductTest {

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
    public AutoApiOffer.CategoryEnum category;

    @Parameter("Создаваемый оффер")
    @Parameterized.Parameter(1)
    public String offerPath;

    @Parameter("Услуга")
    @Parameterized.Parameter(2)
    public List<AutoSalesmanAutoruProduct> product;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {CARS, "offers/dealer_new_cars.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_fresh"))},
                {CARS, "offers/dealer_new_cars.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_special"))},
                {CARS, "offers/dealer_new_cars.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_premium"))},
                {CARS, "offers/dealer_new_cars.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_badge").badges(newArrayList("Бейдж №1", "Бейдж №2", "Бейдж №3")))},
                {MOTO, "offers/dealer_moto.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_fresh"))},
                {MOTO, "offers/dealer_moto.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_special"))},
                {MOTO, "offers/dealer_moto.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_premium"))},
                {MOTO, "offers/dealer_moto.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_badge").badges(newArrayList("Бейдж №1", "Бейдж №2", "Бейдж №3")))},
                {TRUCKS, "offers/dealer_trucks.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_fresh"))},
                {TRUCKS, "offers/dealer_trucks.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_special"))},
                {TRUCKS, "offers/dealer_trucks.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_premium"))},
                {TRUCKS, "offers/dealer_trucks.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_badge").badges(newArrayList("Бейдж №1", "Бейдж №2", "Бейдж №3")))}
        });
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldApplyProduct() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, category, offerPath).getOfferId();

        api.userOffers().applyProducts().categoryPath(category).offerIDPath(offerId)
                .body(new AutoApiApplyAutoruProductsRequest().products(product))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }
}
