package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

import java.util.Arrays;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.CARFAX;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /carfax/bought-reports/by-orders")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class BoughtReportsByOrdersTest {

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
        api.carfax().getBoughtReportsByOrders().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(CARFAX)
    public void shouldSee401WhenNoSession() {
        api.carfax().getBoughtReportsByOrders().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(CARFAX)
    public void shouldHasNoDiffWithProduction() {

        String sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().getBoughtReportsByOrders()
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .createdFromQuery("2021-03-11T17:00:00+03:00")
                .createdToQuery("2022-03-11T17:00:00+03:00")
                .pageQuery(1)
                .pageSizeQuery(5)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        JsonObject resp = req.apply(api);
        JsonObject prodResp = req.apply(prodApi);

        //Если у юзера в тестовой бд нет заказов, помечаем тест как broken, т.к. он перестаёт проверять дифф
        if (resp.getAsJsonArray("reports").size() == 0) {
            throw new RuntimeException("Missing SUCCESS/UPDATING orders for dealer:20101");
        }

        MatcherAssert.assertThat(resp, jsonEquals(prodResp).whenIgnoringPaths(
                Arrays.stream(RawReportUtils.IGNORED_PATHS)
                        .map(p -> p.replaceAll("^report\\.", "reports[*].raw_report."))
                        .toArray(String[]::new)
        ));
    }
}
