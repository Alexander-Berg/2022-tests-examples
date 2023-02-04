package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Arrays;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.CARFAX;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /carfax/bought-reports/raw")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class BoughtReportsRawTest {

    @Inject
    private AccountManager am;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(CARFAX)
    public void shouldSee403WhenNoAuth() {
        api.carfax().getBoughtReportsRaw().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(CARFAX)
    public void shouldSee401WhenNoSession() {
        api.carfax().getBoughtReportsRaw().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(CARFAX)
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String[] vins = new String[]{
                "SALGA2BK0KA531968",
                "X9W8844SCED004591",
                "XTAFS0Y5LF0887451",
                "WBA4X51090FH01738",
                "XW8DA11Z6DK230769",
                "Y6DTF69Y080128583",
                "WDD2462421N045725",
                "XW7D13FV40S013415",
                "WBA8A51070AE76560",
                "WAUZZZ8T9CA021070",
                "XTAGFL130GY028227",
                "WAUZZZ4G8CN061075",
                "VSKJLWR51U0068951",
                "X4X3B19410J119840",
                "WBA6B41020D149056"};
        for (String vin : vins) {
            adaptor.buyVinHistory(sessionId, account.getId(), vin);
        }

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().getBoughtReportsRaw()
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).whenIgnoringPaths(
                Arrays.stream(RawReportUtils.IGNORED_PATHS)
                        .map(p -> p.replaceAll("^report\\.", "reports[*].raw_report."))
                        .toArray(String[]::new)
        ));
    }
}
