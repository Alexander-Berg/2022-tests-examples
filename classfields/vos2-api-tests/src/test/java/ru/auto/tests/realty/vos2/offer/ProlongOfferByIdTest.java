package ru.auto.tests.realty.vos2.offer;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.model.WrappedRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.GetRawOfferResp;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe500WithInternalNumberFormatException;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("POST /api/realty/offer/prolong_offer/{userID}/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class ProlongOfferByIdTest {

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

    @Test
    public void shouldSuccessRestartsOfferTtl() {
        id = adaptor.createOffer(account.getId()).getId().get(0);

        GetRawOfferResp beforeResp = vos2.utils().utilsRawOffer().offerIDPath(id)
                .execute(validatedWith(shouldBe200OkJSON())).as(GetRawOfferResp.class, GSON);

        vos2.offer().prolongOfferRoute().userIDPath(account.getId()).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBeStatusOk()));

        GetRawOfferResp afterResp = vos2.utils().utilsRawOffer().offerIDPath(id)
                .execute(validatedWith(shouldBe200OkJSON())).as(GetRawOfferResp.class, GSON);

        assertThat(afterResp.getTimestampTtlStart(), greaterThanOrEqualTo(beforeResp.getTimestampTtlStart()));
        assertThat(afterResp.getTimestampWillExpire(), greaterThanOrEqualTo(beforeResp.getTimestampWillExpire()));
        assertThat(afterResp.getTimestampUpdate(), greaterThanOrEqualTo(beforeResp.getTimestampUpdate()));
    }

    @Test
    public void shouldSee400WithoutBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().prolongOfferRoute().userIDPath(account.getId()).offerIDPath(id).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        String emptyBody = "{}";
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().prolongOfferRoute().userIDPath(account.getId()).offerIDPath(id).reqSpec(jsonBody(emptyBody))
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404WithInvalidVosUser() {
        id = getRandomLogin();
        vos2.offer().prolongOfferRoute().userIDPath(getRandomString()).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee500WithInvalidOffer() {
        String invalidOfferId = getRandomString();
        vos2.offer().prolongOfferRoute().userIDPath(account.getId()).offerIDPath(invalidOfferId).body(getBodyRequest())
                .execute(validatedWith(shouldBe500WithInternalNumberFormatException(invalidOfferId)));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        vos2.offer().prolongOfferRoute().userIDPath(account.getId()).offerIDPath(randomOfferId).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithOfferNotFound(randomOfferId)));
    }

    private WrappedRequest getBodyRequest() {
        return random(WrappedRequest.class);
    }
}
