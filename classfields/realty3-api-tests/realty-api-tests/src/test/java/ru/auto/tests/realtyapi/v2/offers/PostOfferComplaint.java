package ru.auto.tests.realtyapi.v2.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyComplaintsComplaintRequest;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getEmptyBody;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("POST /offers/{offerId}/complaint")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class PostOfferComplaint {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.offers().createOfferComplaintRoute()
                .offerIdPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidOfferId() {
        api.offers().createOfferComplaintRoute().reqSpec(authSpec())
                .offerIdPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithNoBody() {
        String offerId = adaptor.getOfferIdFromSearcher();

        api.offers().createOfferComplaintRoute().reqSpec(authSpec())
                .offerIdPath(offerId)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee200WithEmptyBody() {
        String offerId = adaptor.getOfferIdFromSearcher();

        api.offers().createOfferComplaintRoute().reqSpec(authSpec())
                .reqSpec(r -> r.setBody(getEmptyBody()))
                .offerIdPath(offerId)
                .execute(validatedWith(shouldBe200Ok()));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee200WithValidData() {
        String offerId = adaptor.getOfferIdFromSearcher();

        api.offers().createOfferComplaintRoute().reqSpec(authSpec())
                .body(random(RealtyComplaintsComplaintRequest.class))
                .offerIdPath(offerId)
                .execute(validatedWith(shouldBe200Ok()));
    }
}
