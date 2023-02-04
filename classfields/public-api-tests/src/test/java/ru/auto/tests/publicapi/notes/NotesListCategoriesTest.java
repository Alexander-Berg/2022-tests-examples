package ru.auto.tests.publicapi.notes;

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
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiOfferListingResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by scrooge on 27.12.17.
 */

@DisplayName("GET /user/notes/{category}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class NotesListCategoriesTest {

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
    @Owner(SCROOGE)
    public void shouldSeeEmptyNotesList() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        api.userNotes().notes().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSeeNotesList() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        adaptor.addNotes(sessionId, category, offerId, getRandomString());

        AutoApiOfferListingResponse response = api.userNotes().notes().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("offers[] должен содержать 1 оффер", response.getOffers(), hasSize(1));
        AutoruApiModelsAssertions.assertThat(response.getOffers().get(0)).hasCategory(category).hasId(offerId);
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSeeNotesListForAnonym() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String anonymSessionId = adaptor.session().getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        adaptor.addNotes(anonymSessionId, category, offerId, getRandomString());

        AutoApiOfferListingResponse response = api.userNotes().notes().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat("offers[] должен содержать 1 оффер", response.getOffers(), hasSize(1));
        AutoruApiModelsAssertions.assertThat(response.getOffers().get(0)).hasCategory(category).hasId(offerId);
    }

    @Test
    @Owner(SCROOGE)
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String anonymSessionId = adaptor.session().getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        adaptor.addNotes(sessionId, category, offerId, getRandomString());

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.userNotes().notes().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
