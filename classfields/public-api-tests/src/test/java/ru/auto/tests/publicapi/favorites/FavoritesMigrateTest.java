package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.api.UserFavoritesApi;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.VertisPassportLoginParameters;
import ru.auto.tests.publicapi.model.VertisPassportLoginResult;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 08.11.17.
 */

@DisplayName("MIGRATE favorites from anonym to auth user")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Issue("VERTISTEST-624")
public class FavoritesMigrateTest {

    private static final int POLL_INTERVAL = 5;
    private static final int TIMEOUT = 60;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private Account account;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    @Description("Проверяем, что после авторизации, избранные переносится авторизованному пользователю")
    public void shouldMigrateFavoritesFromAnonymToAuthUser() {
        String sessionId = adaptor.login(account).getSession().getId();

        VertisPassportSession session = adaptor.session().getSession();
        String anonymSessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();

        api.userFavorites().addFavorite().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).xDeviceUidHeader(deviceUid).execute(validatedWith(shouldBeSuccess()));

        String authSession = api.auth().login().body(new VertisPassportLoginParameters()
                .login(account.getLogin())
                .password(account.getPassword()))
                .xSessionIdHeader(anonymSessionId)
                .xDeviceUidHeader(deviceUid)
                .reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(VertisPassportLoginResult.class).getSession().getId();

        UserFavoritesApi.FavoritesOper favoriteOper = api.userFavorites().favorites().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(authSession).xDeviceUidHeader(deviceUid);

        List<AutoApiOffer> response = given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() -> favoriteOper.executeAs(validatedWith(shouldBeSuccess())).getOffers(), hasSize(1));

        AutoruApiModelsAssertions.assertThat(response.get(0)).hasCategory(category).hasId(offerId);
  }
}
