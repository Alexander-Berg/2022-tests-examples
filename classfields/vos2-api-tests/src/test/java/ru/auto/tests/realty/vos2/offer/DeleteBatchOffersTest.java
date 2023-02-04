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
import ru.auto.tests.realty.vos2.model.DeleteOfferBatchRequest;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithInternalNumberFormatException;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithFailedIds;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("DELETE /api/realty/offer/delete/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class DeleteBatchOffersTest {

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
    public void shouldSuccessDeleteBatchOffers() {
        offerIds = adaptor.createTwoOffers(account.getId());

        vos2.offer().batchDeleteRoute().userIDPath(account.getId()).body(getBodyRequest().offerIds(offerIds))
                .execute(validatedWith(shouldBeStatusOk()));

        offerIds.forEach(id -> vos2.offer().getOfferRoute().userIDPath(account.getId()).offerIDPath(id)
                .execute(validatedWith(shouldBe404WithOfferNotFound(account.getId(), id))));
    }

    @Test
    public void shouldSee404TwiceDeleteBatchOffers() {
        offerIds = adaptor.createTwoOffers(account.getId());

        vos2.offer().batchDeleteRoute().userIDPath(account.getId()).body(getBodyRequest().offerIds(offerIds))
                .execute(validatedWith(shouldBeStatusOk()));
        vos2.offer().batchDeleteRoute().userIDPath(account.getId()).body(getBodyRequest().offerIds(offerIds))
                .execute(validatedWith(shouldBe404WithFailedIds(account.getId(), offerIds)));

        offerIds.forEach(id -> vos2.offer().getOfferRoute().userIDPath(account.getId()).offerIDPath(id)
                .execute(validatedWith(shouldBe404WithOfferNotFound(account.getId(), id))));
    }

    @Test
    public void shouldSuccessDeleteEmptyBatchOffers() {
        vos2.offer().batchDeleteRoute().userIDPath(account.getId()).body(getBodyRequest())
                .execute(validatedWith(shouldBeStatusOk()));
    }

    @Test
    public void shouldSee400WithoutBody() {
        vos2.offer().batchDeleteRoute().userIDPath(account.getId()).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        String emptyBody = "{}";
        vos2.offer().batchDeleteRoute().userIDPath(account.getId()).reqSpec(jsonBody(emptyBody))
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404WithInvalidVosUser() {
        vos2.offer().batchDeleteRoute().userIDPath(getRandomString()).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee400WithInvalidOffer() {
        String invalidOfferId = getRandomString();
        offerIds = adaptor.createTwoOffers(account.getId());
        offerIds.add(invalidOfferId);

        vos2.offer().batchDeleteRoute().userIDPath(account.getId()).body(getBodyRequest().offerIds(offerIds))
                .execute(validatedWith(shouldBe400WithInternalNumberFormatException(invalidOfferId)));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        offerIds = adaptor.createTwoOffers(account.getId());
        offerIds.add(randomOfferId);

        vos2.offer().batchDeleteRoute().userIDPath(account.getId()).body(getBodyRequest().offerIds(offerIds))
                .execute(validatedWith(shouldBe404WithFailedIds(account.getId(), randomOfferId)));
    }

    private DeleteOfferBatchRequest getBodyRequest() {
        DeleteOfferBatchRequest reqBody = random(DeleteOfferBatchRequest.class);
        reqBody.setReason("IR_SOLD_HERE");
        return reqBody;
    }
}
