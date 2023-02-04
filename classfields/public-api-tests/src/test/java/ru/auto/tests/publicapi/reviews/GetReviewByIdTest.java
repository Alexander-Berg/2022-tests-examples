package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
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
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.model.AutoApiReviewResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.REVIEW_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /reviews/{subject}/{reviewId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetReviewByIdTest {

    private final static String DEFAULT_CAR_REVIEW_DRAFT = "reviews_drafts/cars_review.json";

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
    public void shouldSee403WhenNoAuth() {
        api.reviews().readReview().reviewIdPath(Utils.getRandomString()).subjectPath(AUTO)
                .execute((validatedWith(shouldBeCode(SC_FORBIDDEN))));
    }

    @Test
    public void shouldSee401WithoutSessionId() {
        api.reviews().readReview().reviewIdPath(Utils.getRandomString()).subjectPath(AUTO).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldSee404WithIncorrectReviewId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiErrorResponse response = api.reviews().readReview().reviewIdPath(Utils.getRandomString()).subjectPath(AUTO).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeCode(SC_NOT_FOUND))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasStatus(ERROR).hasError(REVIEW_NOT_FOUND).hasDetailedError(REVIEW_NOT_FOUND.name());
    }

    @Test
    public void shouldGetReviewById() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        AutoApiReviewResponse response = api.reviews().readReview().reviewIdPath(reviewId).xSessionIdHeader(sessionId)
                .subjectPath(AUTO).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess())).as(AutoApiReviewResponse.class);

        assertThat(response.getReview()).hasId(reviewId);
    }
}
