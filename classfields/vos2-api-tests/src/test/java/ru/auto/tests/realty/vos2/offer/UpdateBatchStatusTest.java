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
import ru.auto.tests.realty.vos2.model.RealtyUpdateStatusBatchRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.NewStatusEnum.ACTIVE;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.NewStatusEnum.INACTIVE;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.ReasonEnum.SOLD_HERE;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithFailedIds;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithUserNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("PUT /api/realty/offer/update_status/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdateBatchStatusTest {

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
    public void shouldSuccessUpdateActiveStatusWithInactiveStatus() {
        offerIds = adaptor.createTwoOffers(account.getId());

        RealtyUpdateStatusBatchRequest reqBody = getBodyRequest();
        reqBody.getOfferIds().addAll(offerIds);
        reqBody.getStatus().setNewStatus(INACTIVE);

        vos2.offer().batchUpdateStatusRoute().userIDPath(account.getId()).body(reqBody).execute(validatedWith(shouldBeStatusOk()));

        offerIds.forEach(id -> Assertions.assertThat(adaptor.getUserOfferById(account.getId(), id).getOffer()).hasStatus(INACTIVE.getValue()));
    }

    @Test
    public void shouldSuccessUpdateInactiveStatusWithActiveStatus() {
        offerIds = adaptor.createTwoOffers(account.getId());

        RealtyUpdateStatusBatchRequest reqBody = getBodyRequest();
        reqBody.getOfferIds().addAll(offerIds);
        reqBody.getStatus().setNewStatus(INACTIVE);

        vos2.offer().batchUpdateStatusRoute().userIDPath(account.getId()).body(reqBody).execute(validatedWith(shouldBeStatusOk()));

        reqBody.getStatus().setNewStatus(ACTIVE);
        vos2.offer().batchUpdateStatusRoute().userIDPath(account.getId()).body(reqBody).execute(validatedWith(shouldBeStatusOk()));
        offerIds.forEach(id -> adaptor.waitActivateOffer(account.getId(), id));

        offerIds.forEach(id -> Assertions.assertThat(adaptor.getUserOfferById(account.getId(), id).getOffer()).hasStatus(ACTIVE.getValue()));
    }

    @Test
    public void shouldSee400WithoutBody() {
        vos2.offer().batchUpdateStatusRoute().userIDPath(account.getId()).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        vos2.offer().batchUpdateStatusRoute().userIDPath(account.getId()).body(new RealtyUpdateStatusBatchRequest())
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomVosId = getRandomLogin();
        offerIds = adaptor.createTwoOffers(account.getId());
        vos2.offer().batchUpdateStatusRoute().userIDPath(randomVosId).body(getBodyRequest().offerIds(offerIds))
                .execute(validatedWith(shouldBe404WithUserNotFound(randomVosId)));
    }

    @Test
    public void shouldSee404ForInvalidVosUser() {
        vos2.offer().batchUpdateStatusRoute().userIDPath(getRandomString()).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        offerIds = adaptor.createTwoOffers(account.getId());
        offerIds.add(randomOfferId);

        vos2.offer().batchUpdateStatusRoute().userIDPath(account.getId()).body(getBodyRequest().offerIds(offerIds))
                .execute(validatedWith(shouldBe404WithFailedIds(account.getId(), randomOfferId)));
    }

    private RealtyUpdateStatusBatchRequest getBodyRequest() {
        RealtyUpdateStatusBatchRequest body = random(RealtyUpdateStatusBatchRequest.class);
        body.getStatus().setReason(SOLD_HERE);
        return body;
    }
}
