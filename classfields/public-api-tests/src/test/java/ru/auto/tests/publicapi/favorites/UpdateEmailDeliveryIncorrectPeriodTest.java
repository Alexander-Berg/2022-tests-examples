package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiEmailDelivery;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("PUT /user/favorites/all/subscriptions/{id}/email")
@GuiceModules(PublicApiSearchesModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class UpdateEmailDeliveryIncorrectPeriodTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private Account account;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter("Период")
    @Parameterized.Parameter(0)
    public String period;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(new Object[]{"0s", "300s"});
    }

    @Test
    public void shouldSeeErrorWhenIncorrectPeriod() {
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.addSearch(CARS, sessionId).getId();

        AutoApiErrorResponse response = api.userFavorites().upsertEmailDelivery().idPath(id).body(new AutoApiEmailDelivery().enabled(true).period(period))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response)
                .hasError(AutoApiErrorResponse.ErrorEnum.BAD_REQUEST)
                .hasStatus(AutoApiErrorResponse.StatusEnum.ERROR)
                .hasDetailedError(String.format("Email delivery period have incorrect value: %s", period.replace("s", "")));
    }
}
