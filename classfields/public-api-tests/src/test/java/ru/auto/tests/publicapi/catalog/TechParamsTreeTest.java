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
 * Generated API tests for all status codes TechParamsTree
 */
@DisplayName("GET /reference/catalog/cars/tech-param-tree")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class TechParamsTreeTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner("generated")
    @Description("Check status code 400 and response for GET /reference/catalog/cars/tech-param-tree (Ошибка в запросе)")
    public void shouldSee400TechParamsTree() {
        api.catalog().techParamsTree().reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(400)));
    }

    @Test
    @Owner("generated")
    @Description("Check status code 404 and response for GET /reference/catalog/cars/tech-param-tree (Модификация не найдена)")
    public void shouldSee404TechInfo() {
        String mark = Utils.getRandomString();
        String model = Utils.getRandomString();
        String techParamId = Utils.getRandomString();
        api.catalog().techParamsTree().reqSpec(defaultSpec())
                .markQuery(mark)
                .modelQuery(model)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner("generated")
    @Description("Check status code 200 and response for GET /reference/catalog/cars/tech-param-tree (OK)")
    public void shouldSee200TechParamsTree() {

        String mark = "HONDA";
        String model = "CIVIC";
        api.catalog().techParamsTree().reqSpec(defaultSpec())
                .markQuery(mark)
                .modelQuery(model)
                .execute(validatedWith(shouldBeCode(200)));
    }
}
