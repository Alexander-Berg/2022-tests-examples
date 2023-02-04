package ru.auto.tests.realtyapi.v1.phone;


import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.consts.Owners.SCROOGE;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.trueCreateRedirectGeoId;


@Title("GET /phone/canCreateRedirect/{uid}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CanCreateRedirectGeoIdTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Parameter
    @Parameterized.Parameter
    public String geoId;

    @Parameterized.Parameters
    public static Object[] getParameters() {
        return trueCreateRedirectGeoId();
    }

    @Test
    @Issue("REALTYBACK-1199")
    @Owner(SCROOGE)
    public void shouldCanCreateRedirectIsTrue() {
        Account account = am.create();
        Boolean resp = api.phone().canCreateRedirectRoute().reqSpec(authSpec()).uidPath(account.getId()).geoIdQuery(geoId)
                .execute(validatedWith(shouldBe200Ok()))
                .jsonPath().get("response.result[0].canCreateRedirect");

        MatcherAssert.assertThat(String.format("для geoId: '%s' canCreateRedirect не true", geoId), resp, equalTo(true));
    }
}
