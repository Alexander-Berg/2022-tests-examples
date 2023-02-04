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
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiReviewListingResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /user/reviews")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetUserReviewsTest {
    private final static String DEFAULT_CAR_REVIEW_DRAFT = "reviews_drafts/cars_review.json";
    private static final int PAGE = 1;
    private static final int PAGE_SIZE = 10;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.userReviews().userReviews().executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee401WithoutSessionId() {
        api.userReviews().userReviews().reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldEmptyListOfUserReviews() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiReviewListingResponse response = api.userReviews().userReviews()
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.getPagination()).hasTotalOffersCount(0).hasTotalPageCount(0).hasPage(PAGE).hasPageSize(PAGE_SIZE);
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeeOfferReview() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        adaptor.saveReview(account.getLogin(), sessionId, DEFAULT_CAR_REVIEW_DRAFT);

        AutoApiReviewListingResponse response = api.userReviews().userReviews()
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeCode(SC_OK)));

        assertThat(response.getPagination()).hasTotalOffersCount(1).hasTotalPageCount(1).hasPage(PAGE).hasPageSize(PAGE_SIZE);
    }
}
