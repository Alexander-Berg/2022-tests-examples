package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiAddCommentRequest;
import ru.auto.tests.publicapi.model.AutoApiAddCommentResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /reviews/{subject}/{reviewId}/comments")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class AddCommentReviewTest {
    private static final int POLL_INTERVAL = 3;
    private static final int POLL_DELAY = 20;
    private static final int TIMEOUT = 60;
    private final static String DEFAULT_CAR_REVIEW_DRAFT = "reviews_drafts/cars_review.json";
    private final static String DETAILED_ERROR_WITH_EMPTY_BODY = "Пустой текст комментария";
    private final static String DETAILED_ERROR_WITHOUT_SESSION = "Вы не авторизованы! Пожалуйста, авторизуйтесь.";
    private final static String DETAILED_ERROR_TWO_COMMENTS_WITH_NO_TIMEOUT = "Пожалуйста, не отправляйте комментарии слишком часто";
    private final static String DETAILED_ERROR_SHORT_COMMENT = "Слишком короткий комментарий";

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
        api.reviews().addComments().subjectPath(AUTO).reviewIdPath(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectSubject() {
        api.reviews().addComments().subjectPath(Utils.getRandomString()).reviewIdPath(Utils.getRandomString())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutBody() {
        api.reviews().addComments().subjectPath(AUTO).reviewIdPath(Utils.getRandomString()).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithEmptyBody() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();
        AutoApiAddCommentResponse response = api.reviews().addComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .body(new AutoApiAddCommentRequest()).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));

        assertThat(response).hasDetailedError(DETAILED_ERROR_WITH_EMPTY_BODY);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiAddCommentRequest request = new AutoApiAddCommentRequest().message(Utils.getRandomString());
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        AutoApiAddCommentResponse response = api.reviews().addComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .body(request).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));

        assertThat(response).hasDetailedError(DETAILED_ERROR_WITHOUT_SESSION);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithTwoCommentsWithNoTimeout() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        adaptor.addComment(sessionId, AUTO, reviewId, Utils.getRandomString());

        AutoApiAddCommentResponse response = api.reviews().addComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .body(new AutoApiAddCommentRequest().message(Utils.getRandomString())).xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));

        assertThat(response).hasDetailedError(DETAILED_ERROR_TWO_COMMENTS_WITH_NO_TIMEOUT);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithShortComment() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        AutoApiAddCommentResponse response = api.reviews().addComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .body(new AutoApiAddCommentRequest().message(Utils.getRandomString(1))).xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));

        assertThat(response).hasDetailedError(DETAILED_ERROR_SHORT_COMMENT);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldAddComment() {
        String message = Utils.getRandomString();
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        AutoApiAddCommentResponse response = api.reviews().addComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .body(new AutoApiAddCommentRequest().message(message)).xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));

        assertThat(response.getComment()).hasMessage(message);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldAddCommentWithParent() {
        String message = Utils.getRandomString();
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();
        String parentCommentId = adaptor.addComment(sessionId, AUTO, reviewId, Utils.getRandomString()).getComment().getId();

        AutoApiAddCommentResponse response = given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(POLL_DELAY, SECONDS).pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS)
                .ignoreExceptions().until(() -> api.reviews().addComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .body(new AutoApiAddCommentRequest().message(message).parentId(Integer.valueOf(parentCommentId)))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess())), notNullValue());

        assertThat(response.getComment()).hasMessage(message);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldAddCommentWithIncorrectParentId() {
        String message = Utils.getRandomString();
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        AutoApiAddCommentRequest request = new AutoApiAddCommentRequest().message(message).parentId(Utils.getRandomShortInt());
        AutoApiAddCommentResponse response = api.reviews().addComments().subjectPath(AUTO).reviewIdPath(reviewId)
                .body(request).xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));

        assertThat(response.getComment()).hasMessage(message);
    }
}
