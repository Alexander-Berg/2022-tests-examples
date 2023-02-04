package ru.auto.tests.publicapi.autoparts;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated API tests for all status codes SuggestAutoParts
 */
@DisplayName("GET /autoparts/suggest")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class SuggestAutoPartsTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Ignore
    @Owner("generated")
    @Description("Check status code 400 and response for GET /autoparts/suggest (Ошибка в запросе)")
    public void shouldSee400SuggestAutoParts() {
        String category = Utils.getRandomString();
        String text = Utils.getRandomString();

        api.autoparts().suggestAutoParts().reqSpec(defaultSpec())
                .categoryQuery(category)
                .textQuery(text).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
