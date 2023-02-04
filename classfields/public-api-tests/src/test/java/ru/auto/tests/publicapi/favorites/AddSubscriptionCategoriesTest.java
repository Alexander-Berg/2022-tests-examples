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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiSavedSearchCreateParams;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.SUBSCRIPTION_ALREADY_IN_FAVORITE;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("POST /user/favorites/{category}/subscriptions")
@GuiceModules(PublicApiSearchesModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddSubscriptionCategoriesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    public void shouldAddToSearches() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        api.userFavorites().addSavedSearch().categoryPath(category.name()).body(getDefaultBody())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldAddToSearchesForAnonym() {
        String anonymSessionId = adaptor.session().getSession().getId();
        api.userFavorites().addSavedSearch().categoryPath(category.name()).body(getDefaultBody())
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldNotAddTwiceToSearches() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();

        api.userFavorites().addSavedSearch().categoryPath(category.name()).body(getDefaultBody())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        AutoApiErrorResponse response = api.userFavorites().addSavedSearch().categoryPath(category.name()).body(getDefaultBody())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_CONFLICT))).as(AutoApiErrorResponse.class);
        assertThat(response).hasStatus(ERROR)
                .hasError(SUBSCRIPTION_ALREADY_IN_FAVORITE).hasDetailedError(SUBSCRIPTION_ALREADY_IN_FAVORITE.name());
    }


    private AutoApiSavedSearchCreateParams getDefaultBody() {
        return new AutoApiSavedSearchCreateParams()
                .title(Utils.getRandomString())
                .params(new AutoApiSearchSearchRequestParameters());
    }
}