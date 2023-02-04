package ru.auto.tests.cabinet.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.cabinet.module.CabinetApiModule;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /subscription/{id}/client/{client_id}")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(CabinetApiModule.class)
public class GetSubscriptionIdClientCompareTest {

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private CabinetApiAdaptor adaptor;

    @Parameter("Дилер")
    @Parameterized.Parameter(0)
    public String dealerId;

    @Parameter("Пользователь")
    @Parameterized.Parameter(1)
    public String userId;

    @Parameter("Категория")
    @Parameterized.Parameter(2)
    public String category;

    @Parameterized.Parameters(name = "{0} - {1} - {2}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"26210", "22172574", "money"},
                {"26036", "21963722", "info"},
                {"26196", "22160662", "сabinet_update"},
                {"26182", "19656999", "autoload_update"},
                {"26064", "21944046", "autoload"},
                {"26048", "2575349", "legal"},
                {"26044", "21944106", "redemption"}
        });
    }

    @Test
    public void shouldSeeSubscriptionClientById() {
        String email = getRandomEmail();

        adaptor.clearSubscriptions(dealerId, userId);
        Long subscriptionId = adaptor.addSubscription(dealerId, userId, category, email).getId();
        Function<ApiClient, JsonObject> response = apiClient -> apiClient.subscription().getById().clientIdPath(dealerId)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec()).idPath(subscriptionId)
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi)));
    }
}