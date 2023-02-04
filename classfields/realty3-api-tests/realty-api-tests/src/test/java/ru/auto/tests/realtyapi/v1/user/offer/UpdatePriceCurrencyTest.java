package ru.auto.tests.realtyapi.v1.user.offer;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.GetUserOffersByIdResp;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.ExamplePriceModel;
import ru.auto.tests.realtyapi.v1.model.RealtyOfferPrice;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomPrice;



@Title("PUT /user/offers/{offerId}/update_price")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UpdatePriceCurrencyTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Parameter
    @Parameterized.Parameter(0)
    public String currencyIn;

    @Parameter
    @Parameterized.Parameter(1)
    public String currencyOut;

    @Parameterized.Parameters(name = "currencyIn={0} currencyOut={1}")
    public static Object[][] getParameters() {
        return new String[][]{
                {"RUB", "RUR"},
                {"USD", "USD"},
                {"EUR", "EUR"},
                {"UAH", "UAH"},
                {"BYR", "BYR"},
                {"KZT", "KZT"}
        };
    }

    @Test
    public void shouldUpdatePrice() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token).getResponse().getId();
        ExamplePriceModel reqBody = random(ExamplePriceModel.class);
        reqBody.getPrice().currency(RealtyOfferPrice.CurrencyEnum.valueOf(currencyIn)).setValue(getRandomPrice());

        api.userOffers().updatePriceRoute().offerIdPath(offerId).reqSpec(authSpec()).authorizationHeader(token)
                .body(reqBody).execute(validatedWith(shouldBeCode(SC_OK)));

        GetUserOffersByIdResp resp = api.userOffers().getUserOfferRoute().reqSpec(authSpec()).authorizationHeader(token)
                .offerIdPath(offerId).execute(validatedWith(shouldBeCode(SC_OK)))
                .as(GetUserOffersByIdResp.class, GSON);

        assertThat(resp.getResponse().getPrice())
                .hasValue(reqBody.getPrice().getValue().longValue())
                .hasCurrency(currencyOut);
    }
}
