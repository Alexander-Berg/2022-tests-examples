package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiDraftResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.draft.CurrentDraftXUserLocationHeaderTest.IGNORED_PATH;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.TestData.defaultCategories;

@DisplayName("GET /user/draft/{category}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CurrentDraftCategoriesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

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
        return Arrays.asList(defaultCategories());
    }

    @Test
    @Description("Создание черновика, без авторизации")
    public void shouldCreateEmptyDraftForAnonym() {
        VertisPassportSession session = adaptor.session().getSession();

        JsonObject response = api.draft().currentDraft().categoryPath(category.name()).xSessionIdHeader(session.getId())
                .xDeviceUidHeader(session.getDeviceUid()).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        JsonObject responseProd = prodApi.draft().currentDraft().categoryPath(category.name())
                .xSessionIdHeader(session.getId()).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(response, jsonEquals(responseProd).whenIgnoringPaths(IGNORED_PATH));
    }

    @Test
    @Description("Создание черновика при первом запросе")
    public void shouldCreateEmptyDraft() {
        String sessionId = adaptor.login(am.create()).getSession().getId();
        JsonObject response = api.draft().currentDraft().categoryPath(category.name()).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        JsonObject responseProd = prodApi.draft().currentDraft().categoryPath(category.name())
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(response, jsonEquals(responseProd).whenIgnoringPaths(IGNORED_PATH));
    }


    @Test
    @Description("Использование ранее созданного черновика при повторном использовании")
    public void shouldUseCreatedEmptyDraft() {
        String sessionId = adaptor.login(am.create()).getSession().getId();
        AutoApiDraftResponse draftResponse = api.draft().currentDraft().categoryPath(category.name()).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        AutoApiDraftResponse secondDraftResponse = api.draft().currentDraft().categoryPath(category.name()).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        assertThat(secondDraftResponse).hasOfferId(draftResponse.getOfferId());
    }
}
