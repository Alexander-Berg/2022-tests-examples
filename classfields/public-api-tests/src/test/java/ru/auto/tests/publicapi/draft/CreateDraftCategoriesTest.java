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
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiDraftResponse;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.model.AutoApiDraftResponse.StatusEnum.SUCCESS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("POST /user/draft/{category}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreateDraftCategoriesTest {

    static final String[] IGNORED_PATHS = {"offer_id", "offer.state.upload_url", "offer.url", "offer.mobile_url", "offer.price_history[*].create_timestamp", "offer.price_info.create_timestamp",
            "offer.id", "offer.additional_info.expire_date", "offer.additional_info.actualize_date", "offer.additional_info.creation_date", "offer.additional_info.update_date", "offer.user_ref", "offer.private_seller.phones[*].phone",
            "offer.seller.phones[*].phone", "offer.seller.phones[*].original", "offer.private_seller.phones[0].original", "offer.created", "offer.services[*].create_date", "offer.state.panorama_autoru", "offer.state.sts_upload_url", "offer.state.document_photo_upload_urls"};

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
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    public void shouldSuccessCreateDraftWithEmptyBody() {
        Account account = am.create();

        AutoApiLoginResponse loginResult = adaptor.login(account);
        AutoApiDraftResponse saveSuccessResponse = api.draft().createDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .body(new AutoApiOffer())
                .xSessionIdHeader(loginResult.getSession().getId())
                .executeAs(validatedWith(shouldBeSuccess()));
        assertThat(saveSuccessResponse).hasStatus(SUCCESS);
    }

    @Test
    public void shouldSuccessCreateDraftWithEmptyBodyForAnonym() {
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        AutoApiDraftResponse saveSuccessResponse = api.draft().createDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .body(new AutoApiOffer())
                .xSessionIdHeader(sessionId)
                .xDeviceUidHeader(deviceUid)
                .executeAs(validatedWith(shouldBeSuccess()));
        assertThat(saveSuccessResponse).hasStatus(SUCCESS);
    }

    @Test
    public void shouldCreateDraftWithEmptyBodyHasNoDiffWithProduction() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().createDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .body(new AutoApiOffer())
                .xSessionIdHeader(loginResult.getSession().getId())
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths(IGNORED_PATHS));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldCreateDraftWithEmptyBodyHasNoDiffWithProductionForAnonym() {
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().createDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .body(new AutoApiOffer())
                .xSessionIdHeader(sessionId)
                .xDeviceUidHeader(deviceUid)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths(IGNORED_PATHS));
    }
}
