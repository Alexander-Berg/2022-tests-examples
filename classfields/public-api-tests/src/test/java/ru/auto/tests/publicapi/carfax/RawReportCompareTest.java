package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
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

import java.util.Collection;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /carfax/report/raw")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RawReportCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter
    public String vinOrLicensePlate;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<String> getParameters() {
        return newArrayList("SALWA2FK7HA135034", "H568XX36");
    }

    @Test
    @Owner(TIMONDL)
    public void shouldNotBoughtRawReportHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().rawReport()
                .reqSpec(defaultSpec())
                .vinOrLicensePlateQuery(vinOrLicensePlate)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi))
                .whenIgnoringPaths(RawReportUtils.IGNORED_PATHS));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldBoughtRawReportHasNoDiffWithProduction() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.buyVinHistory(sessionId, account.getId(), vinOrLicensePlate);

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().rawReport()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .vinOrLicensePlateQuery(vinOrLicensePlate)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi))
                .whenIgnoringPaths(RawReportUtils.IGNORED_PATHS));
    }
}
