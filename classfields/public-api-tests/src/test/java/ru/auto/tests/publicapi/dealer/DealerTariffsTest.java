package ru.auto.tests.publicapi.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /dealer/tariff")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DealerTariffsTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.dealer().tariff().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee401WithoutSession() {
        api.dealer().tariff().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }
}
