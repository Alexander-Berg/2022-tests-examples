package ru.auto.tests.realty.vos2.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
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
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.Prod;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.model.UpdateOfferInfo;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.model.UpdateOfferInfo.StatusEnum.ACTIVE;
import static ru.auto.tests.realty.vos2.model.UpdateOfferInfo.StatusEnum.DRAFT;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithEntityExpected;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotDraft;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithUserNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe500WithNoSuchElement;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getObjectFromJson;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("PUT /api/realty/draft/update/{userID}/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdateDraftTest {

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

    @Inject
    private Vos2ApiAdaptor adaptor;

    private String id;

    @Test
    public void shouldSuccessUpdateDraft() {
        id = adaptor.createDraft(getUserIdForDraft()).getId().get(0);

        vos2.draft().updateDraftRoute().userIDPath(getUserIdForDraft()).offerIDPath(id)
                .body(getUpdateDraftRequest().status(DRAFT))
                .execute(validatedWith(shouldBeStatusOk()));

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().getDraftRoute()
                .userIDPath(getUserIdForDraft()).offerIDPath(id)
                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    public void shouldSuccessUpdateDraftAndPublish() {
        id = adaptor.createDraft(getUserIdForDraft()).getId().get(0);

        vos2.draft().updateDraftRoute().userIDPath(getUserIdForDraft()).offerIDPath(id)
                .body(getUpdateDraftRequest().status(ACTIVE))
                .execute(validatedWith(shouldBeStatusOk()));

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.offer().getOfferRoute()
                .userIDPath(account.getId()).offerIDPath(id)
                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    public void shouldSee400UpdateDraftWithActiveStatus() {
        id = adaptor.createDraft(getUserIdForDraft()).getId().get(0);

        vos2.draft().updateDraftRoute().userIDPath(getUserIdForDraft()).offerIDPath(id)
                .body(getUpdateDraftRequest().status(ACTIVE))
                .execute(validatedWith(shouldBeStatusOk()));

        vos2.draft().getDraftRoute().userIDPath(getUserIdForDraft()).offerIDPath(id)
                .execute(validatedWith(shouldBe404WithOfferNotDraft(id)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        id = adaptor.createDraft(getUserIdForDraft()).getId().get(0);

        vos2.draft().updateDraftRoute().userIDPath(getUserIdForDraft()).offerIDPath(id)
                .reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBe400WithEntityExpected()));
    }

    @Test
    public void shouldSee500WithEmptyBody() {
        id = adaptor.createDraft(getUserIdForDraft()).getId().get(0);

        vos2.draft().updateDraftRoute().userIDPath(getUserIdForDraft()).offerIDPath(id).body(new UpdateOfferInfo())
                .execute(validatedWith(shouldBe500WithNoSuchElement()));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomVosId = getRandomLogin();
        id = getRandomLogin();

        vos2.draft().updateDraftRoute().userIDPath(format("login:%s", randomVosId)).offerIDPath(id)
                .body(getUpdateDraftRequest().status(DRAFT))
                .execute(validatedWith(shouldBe404WithUserNotFound(randomVosId)));
    }

    @Test
    public void shouldSee404ForInvalidVosUser() {
        String randomVosId = format("login:%s", getRandomString());
        id = getRandomLogin();

        vos2.draft().updateDraftRoute().userIDPath(randomVosId).offerIDPath(id)
                .body(getUpdateDraftRequest().status(DRAFT))
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee404ForNotExistDraft() {
        String randomDraftId = getRandomLogin();

        vos2.draft().updateDraftRoute().userIDPath(getUserIdForDraft()).offerIDPath(randomDraftId)
                .body(getUpdateDraftRequest().status(DRAFT))
                .execute(validatedWith(shouldBe404WithOfferNotFound(randomDraftId)));
    }

    private String getUserIdForDraft() {
        return format("login:%s", account.getId());
    }

    private UpdateOfferInfo getUpdateDraftRequest() {
        return getObjectFromJson(UpdateOfferInfo.class, "testdata/draft_update_request_body.json");
    }
}
