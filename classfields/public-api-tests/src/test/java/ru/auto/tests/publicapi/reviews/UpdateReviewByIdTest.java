package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
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
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiErrorResponseAssert;
import ru.auto.tests.publicapi.model.AutoApiReview;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.REVIEW_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withJsonBody;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("PUT /user/reviews/{reviewId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class UpdateReviewByIdTest {

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
        api.reviews().updateReview().reviewIdPath(Utils.getRandomString()).subjectPath(AUTO)
                .execute((validatedWith(shouldBeCode(SC_FORBIDDEN))));
    }

    @Test
    public void shouldSee400WithoutSessionId() {
        api.reviews().updateReview().reviewIdPath(Utils.getRandomString()).subjectPath(AUTO).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee404WithIncorrectReviewId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        AutoApiErrorResponse response = api.reviews().updateReview().reviewIdPath(Utils.getRandomString()).subjectPath(AUTO)
                .reqSpec(defaultSpec())
                .reqSpec(withJsonBody(format(getResourceAsString(DEFAULT_CAR_REVIEW_DRAFT), account.getLogin())))
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeCode(SC_NOT_FOUND))).as(AutoApiErrorResponse.class);
        AutoApiErrorResponseAssert.assertThat(response).hasStatus(ERROR).hasError(REVIEW_NOT_FOUND).hasDetailedError(REVIEW_NOT_FOUND.name());
    }

    @Test
    public void shouldSeeErrorWhenUpdateWithEmptyBody() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT)
                .getReviewId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.reviews().updateReview()
                .reviewIdPath(reviewId).subjectPath(AUTO)
                .reqSpec(defaultSpec()).body(new AutoApiReview().id(reviewId))
                //success??
                .xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));

    }
}
