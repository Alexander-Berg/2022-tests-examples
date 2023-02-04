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

import java.util.Collection;
import java.util.Arrays;
import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.cabinet.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.cabinet.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /subscriptions/client/{client_id}/category/{category}")
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(CabinetApiModule.class)
public class GetSubscriptionsClientCategoryCompareTest {

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
                {"25646", "21556768", "money"},
                {"25448", "21310194", "info"},
                {"25456", "21325956", "сabinet_update"},
                {"25698", "21650056", "autoload_update"},
                {"25718", "21659742", "autoload"},
                {"25766", "6332224", "legal"},
                {"25856", "21774306", "redemption"}
        });
    }

    @Test
    public void shouldSeeSubscriptionMoney() {
        String email = getRandomEmail();

        adaptor.clearSubscriptions(dealerId, userId);
        adaptor.addSubscription(dealerId, userId, category, email);
        Function<ApiClient, JsonArray> response = apiClient -> apiClient.subscription().getByCategory()
                .clientIdPath(dealerId).categoryPath(category)
                .xAutoruOperatorUidHeader(userId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonArray.class);
        MatcherAssert.assertThat(response.apply(api), jsonEquals(response.apply(prodApi)));
    }

}