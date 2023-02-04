package ru.auto.tests.publicapi.billing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.module.PublicApiModule;

/**
 * Created by vicdev on 15.09.17.
 */

@DisplayName("POST /billing/applepay")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class ApplePayTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Ignore("планируется. Нужна или тестовая карточка с рельными деньгами либо еще что-то")
    public void shouldSee403WhenNoAuth() {
    }
}
