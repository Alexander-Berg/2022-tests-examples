package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiVinCommentsVinReportCommentAssert;
import ru.auto.tests.publicapi.model.AutoApiVinCommentsVinReportCommentResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.CARFAX;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("GET /carfax/user/comment")
@GuiceModules(PublicApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetCommentCompareTest {

    private static final String VIN = "X9FLXXEEBLES67719";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager accountManager;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    @Owner(CARFAX)
    public void shouldSeeComment() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String blockId = getRandomString();
        String commentText = getRandomString(100);
        String offerId = RawReportUtils.createTestOffer(adaptor, account, sessionId);

        adaptor.waitUserOffersActive(sessionId, CARS, VIN);

        adaptor.createCarfaxComment(sessionId, VIN, blockId, commentText);

        AutoApiVinCommentsVinReportCommentResponse response = api.carfax().getComment().reqSpec(defaultSpec())
                .vinQuery(VIN)
                .blockIdQuery(blockId)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        AutoApiVinCommentsVinReportCommentAssert.assertThat(response.getComment())
                .hasText(commentText)
                .hasBlockId(blockId);
    }

    @Test
    @Owner(CARFAX)
    public void shouldGetCommentHasNoDiffWithProduction() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String blockId = getRandomString();
        String offerId = RawReportUtils.createTestOffer(adaptor, account, sessionId);
        adaptor.waitUserOffersActive(sessionId, CARS, VIN);

        adaptor.createCarfaxComment(sessionId, VIN, blockId, getRandomString(100));

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().getComment()
                .reqSpec(defaultSpec())
                .vinQuery(VIN)
                .blockIdQuery(blockId)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
