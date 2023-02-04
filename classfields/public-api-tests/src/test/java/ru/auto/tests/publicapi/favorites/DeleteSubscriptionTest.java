package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NeedAuthentication;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("DELETE /user/favorites/all/subscriptions/{searchId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiSearchesModule.class)
public class DeleteSubscriptionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private Account account;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee403WhenNoAuth() {
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.addSearch(CARS, sessionId).getId();
        api.userFavorites().deleteSavedSearch().searchIdPath(id)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Ignore("Создаем сессию")
    public void shouldSee401WithoutSessionId() {
        String sessionId = adaptor.login(account).getSession().getId();
        String id = adaptor.addSearch(CARS, sessionId).getId();
        api.userFavorites().deleteSavedSearch().searchIdPath(id)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NeedAuthentication()));
    }
}
