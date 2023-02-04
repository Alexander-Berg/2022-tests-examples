package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.junit4.DisplayName;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiPriceAttribute;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Created by dskuznetsov on 25.07.18
 */


@DisplayName("POST /user/offers/{category}/{offerID}/price")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PriceInvalidPricesTest {

    private static final String CURRENCY_RUR = "RUR";
    private static final String DETAILED_ERROR_INVALID_PRICE = "(VALIDATION_ERROR,Цена в рублях должна быть в диапазоне 1500 - 1000000000)";
    private static final int LOWER_LIMIT_PRICE = 1499;
    private static final int UPPER_LIMIT_PRICE = 1000000050;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @Parameter("Цена")
    @Parameterized.Parameter(1)
    public int price;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(provideCategoryAndPrice());
    }

    private static Object[][] provideCategoryAndPrice() {
        return new Object[][]{
                {CARS, LOWER_LIMIT_PRICE},
                {MOTO, LOWER_LIMIT_PRICE},
                {TRUCKS, LOWER_LIMIT_PRICE},
                {CARS, UPPER_LIMIT_PRICE},
                {MOTO, UPPER_LIMIT_PRICE},
                {TRUCKS, UPPER_LIMIT_PRICE},
        };
    }

    @Test
    public void shouldSee400WithInvalidPrice() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();

        AutoApiErrorResponse response = api.userOffers().price().categoryPath(category).offerIDPath(offerId).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .body(new AutoApiPriceAttribute().price(price).currency(CURRENCY_RUR)).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);


        AutoruApiModelsAssertions.assertThat(response).hasDetailedError(DETAILED_ERROR_INVALID_PRICE);
    }
}
