package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Java6Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiOfferListingResponse;
import ru.auto.tests.publicapi.model.AutoApiSavedSearchCreateParams;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("POST /user/favorites/{category}/subscriptions")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiSearchesModule.class)
public class AddSubscriptionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userFavorites().addSavedSearch().categoryPath(CARS.name()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithIncorrectCategory() {
        Account account = am.create();

        String sessionId = adaptor.login(account).getSession().getId();
        String incorrectCategory = Utils.getRandomString();
        api.userFavorites().addSavedSearch().categoryPath(incorrectCategory).body(getDefaultBody())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    @Issue("AUTORUAPI-4700")
    @Owner(DSKUZNETSOV)
    public void shouldSeeSameSavedSearchIdInListing() {
        String anonymSessionId = adaptor.session().getSession().getId();
        Long time = getCurrentTime();

        AutoApiSearchSearchRequestParameters params = api.search().postSearchCars().reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId)
                .body(new AutoApiSearchSearchRequestParameters().creationDateTo(time))
                .executeAs(validatedWith(shouldBeSuccess())).getSearchParameters();

        String resp = api.userFavorites().addSavedSearch().categoryPath(CARS).body(new AutoApiSavedSearchCreateParams().params(params))
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).executeAs(validatedWith(shouldBeSuccess())).getSearch().getId();

        AutoApiOfferListingResponse response = api.search().postSearchCars().reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId)
                .body(new AutoApiSearchSearchRequestParameters().creationDateTo(time + 1L))
                .executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("Отсутствует индикация сохраненности поиска", response.getSavedSearch(), notNullValue());
        Java6Assertions.assertThat(resp).isEqualTo(response.getSavedSearch().getId());
    }

    private AutoApiSavedSearchCreateParams getDefaultBody() {
        return new AutoApiSavedSearchCreateParams()
                .title(Utils.getRandomString())
                .params(new AutoApiSearchSearchRequestParameters());
    }

    private Long getCurrentTime() {
        return (System.currentTimeMillis() / 1000L);
    }
}