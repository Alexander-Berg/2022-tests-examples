package ru.auto.tests.realty.vos2.offer;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
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
import ru.auto.tests.realty.vos2.model.RealtyUpdateStatusRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.GetOfferResp;
import ru.auto.tests.realty.vos2.objects.GetRawOfferResp;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor.getObjectFromJson;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.NewStatusEnum.INACTIVE;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.ReasonEnum;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.ReasonEnum.BY_DEFAULT;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithUserNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultOfferReason;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("PUT /api/realty/offer/update_inactive_reason/{userID}/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdateInactiveReasonByIdTest {

    private static final String REASON_REQUEST_BODY_PATH = "testdata/offer_update_reason_body.json";

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
    public static Object[] reasons() {
        return defaultOfferReason();
    }

    @Test
    @UseDataProvider("reasons")
    public void shouldSuccessUpdateInactiveReason(ReasonEnum reason) {
        id = adaptor.createOffer(account.getId()).getId().get(0);

        RealtyUpdateStatusRequest reqBody = random(RealtyUpdateStatusRequest.class);
        reqBody.getStatus().setNewStatus(INACTIVE);
        reqBody.getStatus().setReason(BY_DEFAULT);
        vos2.offer().updateStatusRoute().userIDPath(account.getId()).offerIDPath(id).body(reqBody)
                .execute(validatedWith(shouldBeStatusOk()));
        GetOfferResp inactiveResp = adaptor.getUserOfferById(account.getId(), id);
        Assertions.assertThat(inactiveResp.getOffer()).hasStatus(INACTIVE.getValue());

        JsonObject body = getBodyRequest();
        body.addProperty("reason", reason.getValue());
        vos2.offer().updateInactiveReasonRoute().userIDPath(account.getId()).offerIDPath(id).reqSpec(jsonBody(body))
                .execute(validatedWith(shouldBeStatusOk()));

        GetRawOfferResp resp = vos2.utils().utilsRawOffer().offerIDPath(id)
                .execute(validatedWith(shouldBe200OkJSON())).as(GetRawOfferResp.class, GSON);

        Assertions.assertThat(resp).hasInactiveReason(reason.getValue());
    }

    @Test
    public void shouldSee400WithoutBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateInactiveReasonRoute().userIDPath(account.getId()).offerIDPath(id)
                .reqSpec(jsonBody(StringUtils.EMPTY)).execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        String emptyBody = "{}";
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateInactiveReasonRoute().userIDPath(account.getId()).offerIDPath(id)
                .reqSpec(jsonBody(emptyBody)).execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomVosId = getRandomLogin();
        id = getRandomString();
        vos2.offer().updateInactiveReasonRoute().userIDPath(randomVosId).offerIDPath(id)
                .reqSpec(jsonBody(getBodyRequest())).execute(validatedWith(shouldBe404WithUserNotFound(randomVosId)));
    }

    @Test
    public void shouldSee404ForInvalidVosUser() {
        String invalidVosId = getRandomString();
        id = getRandomString();
        vos2.offer().updateInactiveReasonRoute().userIDPath(invalidVosId).offerIDPath(id)
                .reqSpec(jsonBody(getBodyRequest()))
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        vos2.offer().updateInactiveReasonRoute().userIDPath(account.getId()).offerIDPath(randomOfferId)
                .reqSpec(jsonBody(getBodyRequest())).execute(validatedWith(shouldBe404WithOfferNotFound(randomOfferId)));
    }

    private JsonObject getBodyRequest() {
        return getObjectFromJson(JsonObject.class, REASON_REQUEST_BODY_PATH);
    }
}
