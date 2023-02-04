package ru.auto.tests.publicapi.billing;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiBillingSchedulesScheduleRequest.ScheduleTypeEnum.ONCE_AT_TIME;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomTime;

/**
* Generated compare test for GetUserDiscount
*/
@DisplayName("GET /billing/services/discount/{category}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetUserDiscountCompareTest {

    private static String DEFAULT_PRODUCT = "all_sale_fresh";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private Account account;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter("Категория")
    @Parameterized.Parameter
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /billing/services/discount/{category}")
    public void shouldGetUserDiscountHasNoDiffWithProduction() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        adaptor.createSchedule(sessionId, category, offerId, DEFAULT_PRODUCT, getRandomTime(), ONCE_AT_TIME);
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.billingServices().getUserDiscount()
            .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .categoryPath(category)
            .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
