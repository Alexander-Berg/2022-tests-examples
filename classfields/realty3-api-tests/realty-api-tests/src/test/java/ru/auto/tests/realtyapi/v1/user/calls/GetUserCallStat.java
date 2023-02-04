package ru.auto.tests.realtyapi.v1.user.calls;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("GET /user/{uid}/call/stat")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetUserCallStat {

    private static final String VALID_LEVEL = "all";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.userCalls().getUserCallsStatRoute()
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidUid() {
        api.userCalls().getUserCallsStatRoute().reqSpec(authSpec())
                .uidPath(getRandomString())
                .levelQuery(VALID_LEVEL)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidLevelQuery() {
        Account account = am.create();

        api.userCalls().getUserCallsStatRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .levelQuery(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldNewUserHasNoCall() {
        Account account = am.create();

        JsonObject response = api.userCalls().getUserCallsStatRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .levelQuery(VALID_LEVEL)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        Assertions.assertThat(response.getAsJsonArray("entries")
                .get(0).getAsJsonObject().getAsJsonObject("stat").get("total").getAsInt())
                .describedAs("Количество звонков должно быть равно нулю")
                .isEqualTo(0);
    }
}
