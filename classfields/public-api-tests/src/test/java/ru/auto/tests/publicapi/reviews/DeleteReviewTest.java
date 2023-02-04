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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiReviewDeleteResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("DELETE /reviews/{subject}/{reviewId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DeleteReviewTest {
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

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.reviews().deleteReview().subjectPath(AUTO).reviewIdPath(Utils.getRandomShortLong())
                .execute((validatedWith(shouldBeCode(SC_FORBIDDEN))));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee404WithIncorrectReviewId() {
        api.reviews().deleteReview().subjectPath(AUTO).reviewIdPath(Utils.getRandomString()).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectSubject() {
        api.reviews().deleteReview().subjectPath(Utils.getRandomString()).reviewIdPath(Utils.getRandomString())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldDeleteReview() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String reviewId = adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT).getReviewId();

        AutoApiReviewDeleteResponse response = api.reviews().deleteReview().subjectPath(AUTO).reviewIdPath(reviewId).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));

        assertThat(response).hasReviewId(reviewId);
    }
}
