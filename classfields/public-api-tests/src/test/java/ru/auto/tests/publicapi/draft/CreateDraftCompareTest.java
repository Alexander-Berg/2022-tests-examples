package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplate;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withJsonBody;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.TestData.defaultOffersByCategories;

@DisplayName("POST /user/draft/{category}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreateDraftCompareTest {

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

    @Parameter("Оффер")
    @Parameterized.Parameter(1)
    public String offer;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0} - {1}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(defaultOffersByCategories());
    }

    @Test
    public void shouldCreateDraftHasNoDiffWithProduction() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        String sessionId = loginResult.getSession().getId();
        String body = new OfferTemplate().process(offer, account.getLogin());

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().createDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .reqSpec(withJsonBody(body))
                .xSessionIdHeader(sessionId)
                .execute(validatedWith((shouldBeSuccess()))).as(JsonObject.class, GSON);


        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths(CreateDraftCategoriesTest.IGNORED_PATHS));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldCreateDraftHasNoDiffWithProductionForAnonym() {
        Account account = am.create();
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        String body = new OfferTemplate().process(offer, account.getLogin());
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().createDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .reqSpec(withJsonBody(body))
                .xSessionIdHeader(sessionId).xDeviceUidHeader(deviceUid).execute(validatedWith((shouldBeSuccess())))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths(CreateDraftCategoriesTest.IGNORED_PATHS));
    }
}
