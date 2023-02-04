package ru.auto.tests.realty.vos2.offer;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.Prod;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.CreateOfferResp;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultOffers;

@DisplayName("POST /api/realty/offer/create/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class CreateOfferTest {

    public static final String OFFER_REQUEST_BODY = "testdata/offer_create_request_body.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient vos2;

    @Inject
    @Prod
    private ApiClient prodVos2;

    @Inject
    @Vos
    private Account account;

    @Inject
    private Vos2ApiAdaptor adaptor;

    @DataProvider
    public static Object[] offers() {
        return defaultOffers();
    }

    @Test
    @UseDataProvider("offers")
    public void shouldSuccessCreateOffer(String offer) {
        CreateOfferResp createResp = vos2.offer().batchCreateRoute().userIDPath(account.getId())
                .reqSpec(jsonBody(getResourceAsString(offer)))
                .execute(validatedWith(shouldBeStatusOk())).as(CreateOfferResp.class, GSON);

        assertThat(createResp.getId().size(), greaterThanOrEqualTo(1));
        adaptor.waitActivateOffer(account.getId(), createResp.getId().get(0));

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.offer().getOfferRoute()
                .userIDPath(account.getId())
                .offerIDPath(createResp.getId().get(0))
                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        vos2.offer().batchCreateRoute().userIDPath(account.getId()).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        String emptyBody = "{}";
        vos2.offer().batchCreateRoute().userIDPath(account.getId()).reqSpec(jsonBody(emptyBody))
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    @Test
    public void shouldSee404WithInvalidVosUser() {
        String randomUserId = getRandomString();
        vos2.offer().batchCreateRoute().userIDPath(randomUserId).reqSpec(jsonBody(getResourceAsString(OFFER_REQUEST_BODY)))
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }
}
