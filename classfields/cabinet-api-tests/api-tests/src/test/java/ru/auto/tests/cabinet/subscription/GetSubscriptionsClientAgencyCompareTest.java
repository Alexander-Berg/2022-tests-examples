package ru.auto.tests.cabinet.subscription;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
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

@DisplayName("GET /subscriptions/agency/{agency_id}/client/{client_id}/category/{category}")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(CabinetApiModule.class)
public class GetSubscriptionsClientAgencyCompareTest {

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

    @Parameter("Категория")
    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameters(name = "{0} - {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {"41850", "money"},
                {"43346", "info"},
                {"20487", "сabinet_update"},
                {"42282", "autoload_update"},
                {"18744", "autoload"},
                {"40656", "legal"},
                {"40095", "redemption"}
        });
    }

    @Test
    public void shouldSeeSubscriptionMoney() {
        String email = getRandomEmail();
        String managerId = "19565983";
        String userAgentId = "14439810";
        String agencyId = "19030";

        adaptor.clearSubscriptions(dealerId, managerId);
        adaptor.addSubscription(dealerId, managerId, category, email);
        Function<ApiClient, JsonArray> response = apiClient -> apiClient.subscription().getByCustomer()
                .clientIdPath(dealerId).categoryPath(category).agencyIdPath(agencyId)
                .xAutoruOperatorUidHeader(userAgentId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonArray.class);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi)));
    }
}