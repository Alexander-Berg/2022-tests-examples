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

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated API tests for all status codes GetParsingOrderStatus
 */
@DisplayName("GET /autoparts/avito/orders/seller/{sellerId}/status")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetParsingOrderStatusTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    /**
     * Пользователь не найден
     */
    @Test
    @Owner("generated")
    @Description("Check status code 404 and response for GET /autoparts/avito/orders/seller/{sellerId}/status (Пользователь не найден)")
    public void shouldSee404GetParsingOrderStatus() {
        String sellerId = Utils.getRandomString();
        api.autoparts().getParsingOrderStatus().reqSpec(defaultSpec())
                .sellerIdPath(sellerId).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }
}
