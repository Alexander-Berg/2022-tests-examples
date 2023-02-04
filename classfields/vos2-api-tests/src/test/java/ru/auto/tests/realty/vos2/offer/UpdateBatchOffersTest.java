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
import ru.auto.tests.realty.vos2.model.UpdateOffersRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import java.util.HashMap;
import java.util.Map;

import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe500WithInternalNumberFormatException;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getObjectFromJson;

@DisplayName("PUT /api/realty/offer/update/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdateBatchOffersTest {

    public static final String UPDATE_BATCH_OFFERS_REQUEST_PATH = "testdata/offer_batch_update_request_body.json";

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

    private Map<Integer, String> offerIds;

    private void createOffers() {
        offerIds = new HashMap<>();
        offerIds.put(0, adaptor.createOffer(account.getId()).getId().get(0));
        offerIds.put(1, adaptor.createOffer(account.getId()).getId().get(0));
    }

    @Test
    public void shouldSuccessUpdateBatchOffers() {
        createOffers();
        UpdateOffersRequest reqBody = getBodyRequest();
        vos2.offer().batchUpdateRoute().userIDPath(account.getId()).body(reqBody).execute(validatedWith(shouldBeStatusOk()));

        offerIds.forEach((ind, id) -> Assertions.assertThat(adaptor.getUserOfferById(account.getId(), id).getSpecific())
                .hasAddress(reqBody.getOffers().get(ind).getLocation().getAddress()));
    }

    @Test
    public void shouldSee500WithInvalidOffer() {
        createOffers();
        String randomId = getRandomString();
        UpdateOffersRequest reqBody = getBodyRequest();
        reqBody.getOffers().get(0).setOfferId(randomId);

        vos2.offer().batchUpdateRoute().userIDPath(account.getId()).body(reqBody)
                .execute(validatedWith(shouldBe500WithInternalNumberFormatException(randomId)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        vos2.offer().batchUpdateRoute().userIDPath(account.getId()).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        String emptyBody = "{}";
        vos2.offer().batchUpdateRoute().userIDPath(account.getId()).reqSpec(jsonBody(emptyBody))
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404WithInvalidVosUser() {
        createOffers();
        vos2.offer().batchUpdateRoute().userIDPath(getRandomString()).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    private UpdateOffersRequest getBodyRequest() {
        UpdateOffersRequest reqBody = getObjectFromJson(UpdateOffersRequest.class, UPDATE_BATCH_OFFERS_REQUEST_PATH);
        offerIds.forEach((ind, id) -> reqBody.getOffers().get(ind).setOfferId(id));
        return reqBody;
    }
}
