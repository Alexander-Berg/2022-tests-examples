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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiEmailDelivery;
import ru.auto.tests.publicapi.model.AutoApiSearchInstance;
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

@DisplayName("PUT /user/favorites/all/subscriptions/{id}/email")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiSearchesModule.class)
public class UpdateEmailDeliveryTest {

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

        AutoApiSearchInstance search = adaptor.addSearch(CARS, sessionId);
        search.title(Utils.getRandomString());
        api.userFavorites().upsertEmailDelivery()
                .idPath(search.getId())
                .body(getEnabledEmailDelivery())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Ignore("Создаем сессию")
    public void shouldSee401WithoutSessionId() {
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiSearchInstance search = adaptor.addSearch(CARS, sessionId);
        search.title(Utils.getRandomString());
        api.userFavorites().upsertEmailDelivery()
                .idPath(search.getId())
                .body(getEnabledEmailDelivery())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NeedAuthentication()));
    }

    private AutoApiEmailDelivery getEnabledEmailDelivery() {
        return new AutoApiEmailDelivery().enabled(true).period("3600s");
    }
}