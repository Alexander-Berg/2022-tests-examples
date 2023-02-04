package ru.auto.tests.realtyapi.v1.event;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("POST /event/phone/{event}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetPhoneEventLog {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.eventsLogging().eventLoggingRoute()
                .eventPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee404WithNoParameters() {
        api.eventsLogging().eventLoggingRoute().reqSpec(authSpec())
                .eventPath(StringUtils.EMPTY)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidEvent() {
        api.eventsLogging().eventLoggingRoute().reqSpec(authSpec())
                .eventPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
