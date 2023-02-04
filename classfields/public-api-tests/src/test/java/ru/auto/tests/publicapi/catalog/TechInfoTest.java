package ru.auto.tests.publicapi.catalog;

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
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated API tests for all status codes TechInfo
 */
@DisplayName("GET /reference/catalog/cars/tech-info")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class TechInfoTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner("generated")
    @Description("Check status code 400 and response for GET /reference/catalog/cars/tech-info (Ошибка в запросе)")
    public void shouldSee400TechInfo() {
        api.catalog().techInfo().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner("generated")
    @Description("Check status code 404 and response for GET /reference/catalog/cars/tech-info (Модификация не найдена)")
    public void shouldSee404TechInfo() {
        String techParamId = Utils.getRandomString();
        api.catalog().techInfo().reqSpec(defaultSpec())
                .techParamIdQuery(techParamId).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}
