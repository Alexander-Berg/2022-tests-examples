package ru.auto.tests.publicapi.autoparts;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated API tests for all status codes GetPropertySets
 */
@DisplayName("GET /autoparts/prop-sets")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetPropertySetsTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner("generated")
    @Description("Check status code 200 and response for GET /autoparts/prop-sets (На все отвечаем 200)")
    public void shouldSee200GetPropertySets() {
        Integer categoryId = Utils.getRandomShortInt();
        String mark = Utils.getRandomString();
        String model = Utils.getRandomString();
        String generation = Utils.getRandomString();
        api.autoparts().getPropertySets().reqSpec(defaultSpec())
                .categoryIdQuery(categoryId)
                .markQuery(mark)
                .modelQuery(model)
                .generationQuery(generation).execute(validatedWith(shouldBeCode(SC_OK)));
    }
}
