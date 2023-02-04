package ru.auto.tests.realty.vos2.offer;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.StringUtils;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.model.RealtyOfferPrice.CurrencyEnum;
import ru.auto.tests.realty.vos2.model.UpdatePriceRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithUserNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultOfferCurrency;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomPrice;

@DisplayName("PUT /api/realty/offer/update_price/{userID}/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdatePriceByIdTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient vos2;

    @Inject
    @Vos
    private Account account;

    @Inject
    private Vos2ApiAdaptor adaptor;

    private String id;

    @DataProvider
    public static Object[] currencys() {
        return defaultOfferCurrency();
    }

    @Test
    @UseDataProvider("currencys")
    public void shouldSuccessUpdatePriceCurrency(CurrencyEnum currency) {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        UpdatePriceRequest reqBody = getBodyRequest();
        reqBody.getPrice().setCurrency(currency);

        vos2.offer().updatePriceRoute().userIDPath(account.getId()).offerIDPath(id).body(reqBody)
                .execute(validatedWith(shouldBeStatusOk()));

        Assertions.assertThat(adaptor.getUserOfferById(account.getId(), id).getSpecific()).hasCurrency(currency.getValue());
    }

    @Test
    public void shouldSuccessUpdatePriceValue() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        UpdatePriceRequest reqBody = getBodyRequest();
        reqBody.getPrice().setValue((double) getRandomPrice());

        vos2.offer().updatePriceRoute().userIDPath(account.getId()).offerIDPath(id).body(reqBody)
                .execute(validatedWith(shouldBeStatusOk()));

        Assertions.assertThat(adaptor.getUserOfferById(account.getId(), id).getSpecific())
                .hasPrice(reqBody.getPrice().getValue().longValue());
    }

    @Test
    public void shouldSee400WithoutBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updatePriceRoute().userIDPath(account.getId()).offerIDPath(id).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updatePriceRoute().userIDPath(account.getId()).offerIDPath(id).body(new UpdatePriceRequest())
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomVosId = getRandomLogin();
        id = getRandomString();
        vos2.offer().updatePriceRoute().userIDPath(randomVosId).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithUserNotFound(randomVosId)));
    }

    @Test
    public void shouldSee404ForInvalidVosUser() {
        String invalidVosId = getRandomString();
        id = getRandomString();
        vos2.offer().updatePriceRoute().userIDPath(invalidVosId).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        vos2.offer().updatePriceRoute().userIDPath(account.getId()).offerIDPath(randomOfferId).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithOfferNotFound(randomOfferId)));
    }

    private UpdatePriceRequest getBodyRequest() {
        return random(UpdatePriceRequest.class);
    }
}

