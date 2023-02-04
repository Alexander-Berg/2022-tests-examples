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
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated API tests for all status codes OptionCodes
 */
@DisplayName("GET /reference/catalog/{category}/option-codes")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class OptionCodesTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Ignore
    @Owner("generated")
    @Description("Check status code 400 and response for GET /reference/catalog/{category}/option-codes (Ошибка в запросе)")
    public void shouldSee400OptionCodes() {
        String category = CARS.getValue();
        api.catalog().optionCodes().reqSpec(defaultSpec())
                .categoryPath(category).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
