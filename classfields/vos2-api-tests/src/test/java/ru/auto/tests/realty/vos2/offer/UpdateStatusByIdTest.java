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
import ru.auto.tests.realty.vos2.model.RealtyUpdateStatusRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.GetOfferResp;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.NewStatusEnum.ACTIVE;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.NewStatusEnum.INACTIVE;
import static ru.auto.tests.realty.vos2.model.RealtyOfferStatusUpdate.ReasonEnum.SOLD_HERE;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithUserNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("PUT /api/realty/offer/update_status/{userID}/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdateStatusByIdTest {

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
    public void shouldSuccessUpdateActiveStatusWithInactiveStatus() {
        RealtyUpdateStatusRequest reqBody = getBodyRequest();
        reqBody.getStatus().setNewStatus(INACTIVE);
        reqBody.getStatus().setReason(SOLD_HERE);

        id = adaptor.createOffer(account.getId()).getId().get(0);

        vos2.offer().updateStatusRoute().userIDPath(account.getId()).offerIDPath(id).body(reqBody)
                .execute(validatedWith(shouldBeStatusOk()));

        GetOfferResp resp = adaptor.getUserOfferById(account.getId(), id);
        Assertions.assertThat(resp.getOffer()).hasStatus(INACTIVE.getValue());
    }

    @Test
    public void shouldSuccessUpdateInactiveStatusWithActiveStatus() {
        RealtyUpdateStatusRequest reqBody = getBodyRequest();
        reqBody.getStatus().setNewStatus(INACTIVE);
        reqBody.getStatus().setReason(SOLD_HERE);

        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateStatusRoute().userIDPath(account.getId()).offerIDPath(id).body(reqBody)
                .execute(validatedWith(shouldBeStatusOk()));

        reqBody.getStatus().setNewStatus(ACTIVE);
        vos2.offer().updateStatusRoute().userIDPath(account.getId()).offerIDPath(id).body(reqBody)
                .execute(validatedWith(shouldBeStatusOk()));

        adaptor.waitActivateOffer(account.getId(), id);

        GetOfferResp resp = adaptor.getUserOfferById(account.getId(), id);
        Assertions.assertThat(resp.getOffer()).hasStatus(ACTIVE.getValue());
    }

    @Test
    public void shouldSee400WithoutBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateStatusRoute().userIDPath(account.getId()).offerIDPath(id).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().updateStatusRoute().userIDPath(account.getId()).offerIDPath(id).body(new RealtyUpdateStatusRequest())
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomVosId = getRandomLogin();
        id = getRandomString();
        vos2.offer().updateStatusRoute().userIDPath(randomVosId).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithUserNotFound(randomVosId)));
    }

    @Test
    public void shouldSee404ForInvalidVosUser() {
        String invalidVosId = getRandomString();
        id = getRandomString();
        vos2.offer().updateStatusRoute().userIDPath(invalidVosId).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        vos2.offer().updateStatusRoute().userIDPath(account.getId()).offerIDPath(randomOfferId).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithOfferNotFound(randomOfferId)));
    }

    private RealtyUpdateStatusRequest getBodyRequest() {
        return random(RealtyUpdateStatusRequest.class);
    }
}
