package ru.auto.tests.realtyapi.v2.cabinet;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.ME;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.getBillingDomain;

@Title("GET /cabinet/{billing-domain}/user/{user}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetUserInfoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameterized.Parameter(0)
    public String domain;

    @Parameterized.Parameters(name = "billing-domain={0}")
    public static Object[] getParameters() {
        return getBillingDomain();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.cabinet().userInfo()
                .billingDomainPath(domain)
                .userPath(ME)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoUser() {
        api.cabinet().userInfo().reqSpec(authSpec())
                .billingDomainPath(domain)
                .userPath(EMPTY)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoUid() {
        api.cabinet().userInfo().reqSpec(authSpec())
                .billingDomainPath(domain)
                .userPath(ME)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }
}
