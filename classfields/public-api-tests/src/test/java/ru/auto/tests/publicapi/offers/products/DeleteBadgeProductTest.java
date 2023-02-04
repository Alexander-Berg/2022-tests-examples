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
import ru.auto.tests.publicapi.model.AutoSalesmanAutoruProduct;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("DELETE /user/offers/{category}/{offerID}/products")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiDealerModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DeleteBadgeProductTest {

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
                {CARS, "offers/dealer_new_cars.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_badge").badges(newArrayList("Бейдж №1", "Бейдж №2", "Бейдж №3")))},
                {MOTO, "offers/dealer_moto.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_badge").badges(newArrayList("Бейдж №1", "Бейдж №2", "Бейдж №3")))},
                {TRUCKS, "offers/dealer_trucks.json", newArrayList(new AutoSalesmanAutoruProduct().code("all_sale_badge").badges(newArrayList("Бейдж №1", "Бейдж №2", "Бейдж №3")))}
        });
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldDeleteProduct() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, category, offerPath).getOfferId();
        adaptor.applyBadgeProduct(product, category, offerId, sessionId);

        api.userOffers().deleteProducts().categoryPath(category).offerIDPath(offerId).productQuery(product.get(0).getCode())
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }
}
