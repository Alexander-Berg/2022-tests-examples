package ru.auto.tests.publicapi.offers;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("GET /user/offers/{category}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MarkModelInfoTest {
    private static final int POLL_INTERVAL = 2;
    private static final int TIMEOUT = 30;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @Parameter("JsonPath")
    @Parameterized.Parameter(1)
    public String jsonPath;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(provideCategoriesWithMarkModelInfoPath());
    }

    public static Object[][] provideCategoriesWithMarkModelInfoPath() {
        return new Object[][]{
                {CARS, "offers[0].car_info.mark_info.name"},
                {MOTO, "offers[0].moto_info.mark_info.name"},
                {TRUCKS, "offers[0].truck_info.mark_info.name"},
                {CARS, "offers[0].car_info.model_info.name"},
                {MOTO, "offers[0].moto_info.model_info.name"},
                {TRUCKS, "offers[0].truck_info.model_info.name"}
        };
    }

    @Test
    @Owner(DSKUZNETSOV)
    @Issue("AUTORUAPI-4315")
    public void shouldSeeMarkAndModelInfo() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.createOffer(account.getLogin(), sessionId, category);

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() -> api.userOffers().offers().categoryPath(category.name())
                        .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                        .executeAs(validatedWith(shouldBeSuccess())).getOffers().size(), equalTo(1));

        String response = api.userOffers().offers().categoryPath(category.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess())).jsonPath().get(jsonPath);

        MatcherAssert.assertThat(String.format("в ответе отсутствует %s [AUTORUAPI-4315] ", jsonPath),
                response, notNullValue());
    }
}
