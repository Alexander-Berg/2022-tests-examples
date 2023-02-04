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
import ru.auto.tests.realty.vos2.model.ProlongOffersRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.GetRawOfferResp;

import java.util.List;

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
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithFailedIds;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe500WithInternalNumberFormatException;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("POST /api/realty/offer/prolong_offers/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class ProlongOffersTest {

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

    private List<String> offerIds;

    @Test
    public void shouldSuccessRestartsBatchOffersTtl() {
        offerIds = adaptor.createTwoOffers(account.getId());

        GetRawOfferResp beforeResp = vos2.utils().utilsRawOffer().offerIDPath(offerIds.get(0))
                .execute(validatedWith(shouldBe200OkJSON())).as(GetRawOfferResp.class, GSON);

        vos2.offer().prolongOffersRoute().userIDPath(account.getId()).body(getBodyRequest().offers(offerIds))
                .execute(validatedWith(shouldBeStatusOk()));

        GetRawOfferResp afterResp = vos2.utils().utilsRawOffer().offerIDPath(offerIds.get(0))
                .execute(validatedWith(shouldBe200OkJSON())).as(GetRawOfferResp.class, GSON);

        assertThat(afterResp.getTimestampTtlStart(), greaterThanOrEqualTo(beforeResp.getTimestampTtlStart()));
        assertThat(afterResp.getTimestampWillExpire(), greaterThanOrEqualTo(beforeResp.getTimestampWillExpire()));
        assertThat(afterResp.getTimestampUpdate(), greaterThanOrEqualTo(beforeResp.getTimestampUpdate()));
    }

    @Test
    public void shouldSuccessRestartsTtlWithEmptyBatchOffers() {
        vos2.offer().prolongOffersRoute().userIDPath(account.getId()).body(getBodyRequest())
                .execute(validatedWith(shouldBeStatusOk()));
    }

    @Test
    public void shouldSee400WithoutBody() {
        vos2.offer().prolongOffersRoute().userIDPath(account.getId()).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        String emptyBody = "{}";
        vos2.offer().prolongOffersRoute().userIDPath(account.getId()).reqSpec(jsonBody(emptyBody))
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404WithInvalidVosUser() {
        vos2.offer().prolongOffersRoute().userIDPath(getRandomString()).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee500WithInvalidOffer() {
        String invalidOfferId = getRandomString();
        offerIds = adaptor.createTwoOffers(account.getId());
        offerIds.add(invalidOfferId);

        vos2.offer().prolongOffersRoute().userIDPath(account.getId()).body(getBodyRequest().offers(offerIds))
                .execute(validatedWith(shouldBe500WithInternalNumberFormatException(invalidOfferId)));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        offerIds = adaptor.createTwoOffers(account.getId());
        offerIds.add(randomOfferId);

        vos2.offer().prolongOffersRoute().userIDPath(account.getId()).body(getBodyRequest().offers(offerIds))
                .execute(validatedWith(shouldBe404WithFailedIds(account.getId(), randomOfferId)));
    }

    private ProlongOffersRequest getBodyRequest() {
        return random(ProlongOffersRequest.class);
    }
}