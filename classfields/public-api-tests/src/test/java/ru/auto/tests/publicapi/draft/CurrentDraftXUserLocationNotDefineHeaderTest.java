package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
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
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.draft.CurrentDraftXUserLocationHeaderTest.IGNORED_PATH;
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
public class CurrentDraftXUserLocationNotDefineHeaderTest {

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

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<String> getParameters() {
        return provideHeadersValue();
    }

    private static List<String> provideHeadersValue() {
        return newArrayList(
                "lat=1;lon=0;acc=0",
                "lat=1;lon=1;acc=0",
                "lat=0;lon=0;acc=0"
        );
    }

    @Test
    @Issue("AUTORUAPI-3639")
    public void shouldNotDefineGeoViaUserLocationHeader() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().currentDraft().categoryPath(CARS.name()).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .reqSpec(xUserLocationHeader(headerValue))
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths(IGNORED_PATH));
    }
}
