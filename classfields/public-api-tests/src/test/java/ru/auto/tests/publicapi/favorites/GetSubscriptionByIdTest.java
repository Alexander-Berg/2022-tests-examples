package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.model.AutoApiSavedSearchResponse;
import ru.auto.tests.publicapi.model.AutoApiSearchInstanceAssert;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.SUBSCRIPTION_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /user/favorites/all/subscriptions/{id}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiSearchesModule.class)
public class GetSubscriptionByIdTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userFavorites().getSavedSearch().idPath(Utils.getRandomString()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void should404ForNotExistId() {
        String id = Utils.getRandomString();
        AutoApiErrorResponse response = api.userFavorites().getSavedSearch().idPath(id)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)))
                .as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasStatus(AutoApiErrorResponse.StatusEnum.ERROR)
                .hasError(SUBSCRIPTION_NOT_FOUND)
                .hasDetailedError(SUBSCRIPTION_NOT_FOUND.name());
    }


    @Test
    public void shouldSeeSubscription() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.addSearch(CARS, sessionId).getId();

        AutoApiSavedSearchResponse response = api.userFavorites().getSavedSearch().idPath(id)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));
        AutoApiSearchInstanceAssert.assertThat(response.getSearch()).hasId(id);
    }

    @Test
    public void shouldSeeSubscriptionForAnonym() {
        String sessionId = adaptor.session().getSession().getId();
        String id = adaptor.addSearch(CARS, sessionId).getId();

        AutoApiSavedSearchResponse response = api.userFavorites().getSavedSearch().idPath(id)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));

        AutoApiSearchInstanceAssert.assertThat(response.getSearch()).hasId(id);
    }
}
