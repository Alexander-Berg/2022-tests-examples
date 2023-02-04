package ru.auto.tests.publicapi.dealer;

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
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getAtcBelgorodAccount;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMajorAccount;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMaseratiUralAccount;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getMercedesIrkAccount;

@DisplayName("GET /dealer/tariff")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DealerTariffCompareTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Аккаунт")
    @Parameterized.Parameter
    public Account account;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Account> getParameters() {
        return newArrayList(
                getMajorAccount(),
                getMaseratiUralAccount(),
                getAtcBelgorodAccount(),
                getMercedesIrkAccount());
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldGetTariffHasNoDiffWithProduction() {
        String sessionId = adaptor.login(account).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.dealer().tariff()
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
