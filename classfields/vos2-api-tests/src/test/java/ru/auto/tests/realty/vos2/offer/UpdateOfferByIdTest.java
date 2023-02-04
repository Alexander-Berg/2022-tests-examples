package ru.auto.tests.realty.vos2.offer;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.realty.vos2.model.UpdateOfferInfo;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.GetOfferResp;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.model.UpdateOfferInfo.StatusEnum.DRAFT;
import static ru.auto.tests.realty.vos2.model.UpdateOfferInfo.StatusEnum.INACTIVE;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithMissingCommon;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotDraftInOffer;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithUserNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getObjectFromJson;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("PUT /api/realty/offer/update/{userID}/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdateOfferByIdTest {

    private static final String UPDATE_OFFER_REQUEST_PATH = "testdata/offer_update_request_body.json";

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
    public void shouldSuccessUpdateOfferWithInactiveStatus() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateRoute().userIDPath(account.getId()).offerIDPath(id).body(getBodyRequest().status(INACTIVE))
                .execute(validatedWith(shouldBeStatusOk()));

        GetOfferResp resp = vos2.offer().getOfferRoute().userIDPath(account.getId()).offerIDPath(id)
                .execute(validatedWith(shouldBeStatusOk())).as(GetOfferResp.class, GSON);

        Assertions.assertThat(resp.getOffer()).hasStatus(INACTIVE.getValue());
    }

    @Test
    public void shouldSee400WithNotDraftInOffer() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateRoute().userIDPath(account.getId()).offerIDPath(id).body(getBodyRequest().status(DRAFT))
                .execute(validatedWith(shouldBe400WithNotDraftInOffer()));
    }

    @Test
    public void shouldSee400WithoutBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateRoute().userIDPath(account.getId()).offerIDPath(id).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateRoute().userIDPath(account.getId()).offerIDPath(id).body(new UpdateOfferInfo())
                .execute(validatedWith(shouldBe400WithMissingCommon()));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomVosId = getRandomLogin();
        id = getRandomString();
        vos2.offer().updateRoute().userIDPath(randomVosId).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithUserNotFound(randomVosId)));
    }

    @Test
    public void shouldSee404ForInvalidVosUser() {
        String invalidVosId = getRandomString();
        id = getRandomString();
        vos2.offer().updateRoute().userIDPath(invalidVosId).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        vos2.offer().updateRoute().userIDPath(account.getId()).offerIDPath(randomOfferId).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithOfferNotFound(randomOfferId)));
    }

    private UpdateOfferInfo getBodyRequest() {
        return getObjectFromJson(UpdateOfferInfo.class, UPDATE_OFFER_REQUEST_PATH);
    }
}
