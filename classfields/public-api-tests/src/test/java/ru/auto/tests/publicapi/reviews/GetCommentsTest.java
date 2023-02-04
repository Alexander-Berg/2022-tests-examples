package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
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
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiAddCommentResponse;
import ru.auto.tests.publicapi.model.AutoApiCommentListingResponse;
import ru.auto.tests.publicapi.model.AutoApiReviewComment;
import ru.auto.tests.publicapi.module.PublicApiModule;
import org.assertj.core.api.Assertions;
import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.model.AutoApiReviewComment.StatusEnum.REMOVED;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /reviews/{subject}/{reviewId}/comments")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetCommentsTest {
    private final static String DEFAULT_CAR_REVIEW_DRAFT = "reviews_drafts/cars_review.json";
    private static final int PAGE = 1;
    private static final int PAGE_SIZE = 10;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.reviews().getReveiwComments().subjectPath(AUTO).reviewIdPath(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectSubject() {
        api.reviews().getReveiwComments().subjectPath(Utils.getRandomString()).reviewIdPath(Utils.getRandomString())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectReviewId() {
        api.reviews().getReveiwComments().subjectPath(AUTO).reviewIdPath(Utils.getRandomString())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee200WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();
        AutoApiCommentListingResponse response = api.reviews().getReveiwComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));

        Assertions.assertThat(response.getReviewComments()).isNull();
        assertThat(response.getPagination()).hasPage(PAGE).hasPageSize(PAGE_SIZE);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeeCommentsListing() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();
        AutoApiAddCommentResponse comment = adaptor.addComment(sessionId, AUTO, reviewId, Utils.getRandomString());

        AutoApiCommentListingResponse response = api.reviews().getReveiwComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat(response.getReviewComments(), hasSize(1));
        assertThat(response.getReviewComments().get(0))
                .hasMessage(comment.getComment().getMessage()).hasId(comment.getComment().getId());
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeeDeletedCommentInListing() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();
        String commentId = adaptor.addComment(sessionId, AUTO, reviewId, Utils.getRandomString()).getComment().getId();
        adaptor.deleteComment(sessionId, AUTO, reviewId, commentId);

        AutoApiCommentListingResponse response = api.reviews().getReveiwComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));

        System.out.println(response);
        Assertions.assertThat(response.getReviewComments()).isNull();
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeeCommentsListingWithParent() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();
        String parentCommentId = adaptor.addComment(sessionId, AUTO, reviewId, Utils.getRandomString()).getComment().getId();
        AutoApiAddCommentResponse childCommentResponse = adaptor.addChildComment(sessionId, AUTO, reviewId, parentCommentId, Utils.getRandomString());

        AutoApiCommentListingResponse responseListing = api.reviews().getReveiwComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));

        assertThat(responseListing.getReviewComments().get(0).getComments().get(0))
                .hasMessage(childCommentResponse.getComment().getMessage())
                .hasId(childCommentResponse.getComment().getId());
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldNoDiffWithProduction() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();
        String parentCommentId = adaptor.addComment(sessionId, AUTO, reviewId, Utils.getRandomString()).getComment().getId();
        adaptor.addChildComment(sessionId, AUTO, reviewId, parentCommentId, Utils.getRandomString());

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.reviews().getReveiwComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
