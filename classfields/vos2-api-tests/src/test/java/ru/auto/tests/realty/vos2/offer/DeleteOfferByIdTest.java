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

@DisplayName("DELETE /api/realty/offer/delete/{userID}/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class DeleteOfferByIdTest {

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
    public void shouldSuccessDeleteOfferById() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().deleteRoute().userIDPath(account.getId()).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBeStatusOk()));

        vos2.offer().getOfferRoute().userIDPath(account.getId()).offerIDPath(id)
                .execute(validatedWith(shouldBe404WithOfferNotFound(account.getId(), id)));
    }

    @Test
    public void shouldSee404TwiceDeleteOfferById() {
        id = adaptor.createOffer(account.getId()).getId().get(0);

        vos2.offer().deleteRoute().userIDPath(account.getId()).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBeStatusOk()));
        vos2.offer().deleteRoute().userIDPath(account.getId()).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithFailedIds(account.getId(), id)));

        vos2.offer().getOfferRoute().userIDPath(account.getId()).offerIDPath(id)
                .execute(validatedWith(shouldBe404WithOfferNotFound(account.getId(), id)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().deleteRoute().userIDPath(account.getId()).offerIDPath(id).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));

    }

    @Test
    public void shouldSee400WithEmptyBody() {
        String emptyBody = "{}";
        id = adaptor.createOffer(account.getId()).getId().get(0);
        vos2.offer().deleteRoute().userIDPath(account.getId()).offerIDPath(id).reqSpec(jsonBody(emptyBody))
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404WithInvalidVosUser() {
        id = getRandomString();
        vos2.offer().deleteRoute().userIDPath(getRandomString()).offerIDPath(id).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee400WithInvalidOffer() {
        String invalidOfferId = getRandomString();
        vos2.offer().deleteRoute().userIDPath(account.getId()).offerIDPath(invalidOfferId).body(getBodyRequest())
                .execute(validatedWith(shouldBe400WithInternalNumberFormatException(invalidOfferId)));
    }

    @Test
    public void shouldSee404ForNotExistOffer() {
        String randomOfferId = getRandomLogin();
        vos2.offer().deleteRoute().userIDPath(account.getId()).offerIDPath(randomOfferId).body(getBodyRequest())
                .execute(validatedWith(shouldBe404WithFailedIds(account.getId(), randomOfferId)));
    }

    private WrappedRequest getBodyRequest() {
        return random(WrappedRequest.class);
    }
}

