package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.api.UserReviewsApi;
import ru.auto.tests.publicapi.model.AutoApiReviewResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.awaitility.Awaitility.given;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.model.AutoApiReview.UserOpinionEnum.DISLIKE;
import static ru.auto.tests.publicapi.model.AutoApiReview.UserOpinionEnum.LIKE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /reviews/{subject}/{reviewId}/opinion/{opinion}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class AddOpinionTest {
    private final static String DEFAULT_CAR_REVIEW_DRAFT = "reviews_drafts/cars_review.json";
    private static final int POLL_INTERVAL = 5;
    private static final int POLL_DELAY = 10;
    private static final int TIMEOUT = 90;

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
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.reviews().setOpinion().subjectPath(Utils.getRandomString()).reviewIdPath(Utils.getRandomString())
                .opinionPath(Utils.getRandomString()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee200WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        api.reviews().setOpinion().subjectPath(AUTO).reviewIdPath(reviewId)
                .opinionPath(LIKE).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectSubject() {
        api.reviews().setOpinion().subjectPath(Utils.getRandomString()).reviewIdPath(Utils.getRandomString())
                .opinionPath(LIKE).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectOpinion() {
        api.reviews().setOpinion().subjectPath(AUTO).reviewIdPath(Utils.getRandomString())
                .opinionPath(Utils.getRandomString()).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSetOnlyOneOpinion() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        api.reviews().setOpinion().subjectPath(AUTO).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .reviewIdPath(reviewId).opinionPath(DISLIKE).execute(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));
        api.reviews().setOpinion().subjectPath(AUTO).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .reviewIdPath(reviewId).opinionPath(LIKE).execute(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));

        waitLike(sessionId, reviewId);

        MatcherAssert.assertThat(adaptor.getUserReview(sessionId, reviewId).getReview().getDislikeNum(), Matchers.is(Matchers.nullValue()));
    }

    public void waitLike(String sessionId, String reviewId) {
        UserReviewsApi.UserReviewOper reviewOper = api.userReviews().userReview().reviewIdPath(reviewId).xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec());

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(POLL_DELAY, SECONDS)
                .pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS).ignoreExceptions().untilAsserted(() ->
                MatcherAssert.assertThat(reviewOper.execute(validatedWith(shouldBeSuccess())).as(AutoApiReviewResponse.class)
                        .getReview().getLikeNum(), Matchers.equalTo(1)));
    }
}