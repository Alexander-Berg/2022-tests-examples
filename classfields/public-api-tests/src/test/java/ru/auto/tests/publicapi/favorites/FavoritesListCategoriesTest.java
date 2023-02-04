package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiFavoriteListingResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 22.09.17.
 */

@DisplayName("GET /user/favorites/{category}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoritesListCategoriesTest {

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

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    public void shouldSeeEmptyFavoritesList() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        api.userFavorites().favorites().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSeeEmptyFavoritesListForAnonym() {
        String anonymSessionId = adaptor.session().getSession().getId();
        api.userFavorites().favorites().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    public void shouldSeeFavoritesList() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        adaptor.addFavorites(sessionId, category, offerId);

        AutoApiFavoriteListingResponse response = api.userFavorites().favorites().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("offers[] должен содержать 1 оффер", response.getOffers(), hasSize(1));
        AutoruApiModelsAssertions.assertThat(response.getOffers().get(0)).hasCategory(category).hasId(offerId);
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSeeFavoritesListForAnonym() {
        Account account = am.create();
        String anonymSessionId = adaptor.session().getSession().getId();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        adaptor.addFavorites(anonymSessionId, category, offerId);

        AutoApiFavoriteListingResponse response = api.userFavorites().favorites().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("offers[] должен содержать 1 оффер", response.getOffers(), hasSize(1));
        AutoruApiModelsAssertions.assertThat(response.getOffers().get(0)).hasCategory(category).hasId(offerId);
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String anonymSessionId = adaptor.session().getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        adaptor.addFavorites(anonymSessionId, category, offerId);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.userFavorites().favorites().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
