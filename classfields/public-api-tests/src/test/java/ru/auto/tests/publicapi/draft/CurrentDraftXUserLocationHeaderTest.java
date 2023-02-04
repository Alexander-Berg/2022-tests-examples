package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiDraftResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.xUserLocationHeader;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 19.09.17.
 */

@DisplayName("GET /user/draft/{category}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CurrentDraftXUserLocationHeaderTest {

    static final String[] IGNORED_PATH = new String[]{"offer.state.upload_url", "offer.state.sts_upload_url", "offer.state.document_photo_upload_urls"};

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter("Header")
    @Parameterized.Parameter(0)
    public String headerValue;

    @Parameter("geobaseId")
    @Parameterized.Parameter(1)
    public long expectedGeobaseid;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(provideHeadersValue());
    }

    private static Object[] provideHeadersValue() {
        return new Object[][]{
                {"lat=59.9596322;lon=30.4064225;acc=23.419", 2},
                {"lat=55.7522200;lon=37.6155600;acc=23.419", 213}
        };
    }

    @Test
    @Issue("AUTORUAPI-3639")
    public void shouldHasNoDiffLocations() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().currentDraft().categoryPath(CARS.name()).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .reqSpec(xUserLocationHeader(headerValue))
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths(IGNORED_PATH));

    }

    @Test
    @Issue("AUTORUAPI-3639")
    public void shouldDefineGeoViaUserLocationHeader() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiDraftResponse response = api.draft().currentDraft().categoryPath(CARS.name()).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .reqSpec(xUserLocationHeader(headerValue))
                .executeAs(validatedWith(shouldBeSuccess()));

        assertThat(response.getOffer().getSeller().getLocation()).hasGeobaseId(expectedGeobaseid);
        assertThat(response.getOffer().getPrivateSeller().getLocation()).hasGeobaseId(expectedGeobaseid);
    }
}
