package ru.auto.tests.realty.vos2.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.anno.Prod;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;
import ru.auto.tests.realty.vos2.objects.CreateDraftResp;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultOffers;

@DisplayName("POST /api/realty/draft/create/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class CreateDraftTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient vos2;

    @Inject
    @Prod
    private ApiClient prodVos2;

    @Inject
    @Vos
    private Account account;

    @DataProvider
    public static Object[] drafts() {
        return defaultOffers();
    }

    @Test
    @UseDataProvider("drafts")
    public void shouldSuccessCreateDraft(String draft) {
        CreateDraftResp createResp = vos2.draft().batchCreateDraftsRoute().userIDPath(getUserIdForDraft())
                .reqSpec(jsonBody(getResourceAsString(draft)))
                .execute(validatedWith(shouldBeStatusOk())).as(CreateDraftResp.class, GSON);

        assertThat(createResp.getId().size(), greaterThanOrEqualTo(1));

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().getDraftRoute()
                .userIDPath(getUserIdForDraft())
                .offerIDPath(createResp.getId().get(0))
                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        vos2.draft().batchCreateDraftsRoute().userIDPath(getUserIdForDraft()).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        String emptyBody = "{}";
        vos2.draft().batchCreateDraftsRoute().userIDPath(getUserIdForDraft()).reqSpec(jsonBody(emptyBody))
                .execute(validatedWith(shouldBe400WithNotValidJSON()));
    }

    private String getUserIdForDraft() {
        return format("login:%s", account.getId());
    }
}
