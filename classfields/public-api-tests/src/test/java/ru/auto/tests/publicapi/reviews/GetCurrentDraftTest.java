package ru.auto.tests.publicapi.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
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
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.stream.Collectors;

import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.hasItem;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /reviews/{subject}/draft")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)

public class GetCurrentDraftTest {
    private final static String DEFAULT_CAR_REVIEW_DRAFT = "reviews_drafts/cars_review.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee404WhenNoCurrentDraft() {
        Account account = am.create();
        AutoApiLoginResponse loginResult = adaptor.login(account);
        api.reviews().getCurrentDraft().subjectPath(AUTO).reqSpec(defaultSpec())
                .xSessionIdHeader(loginResult.getSession().getId()).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectSubject() {
        api.reviews().getCurrentDraft().subjectPath(Utils.getRandomString()).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    @Description("Создаем драфт анонима. Логинимся на новый акк (на котором отсутствует драфт). " +
            "Драфта нет")
    public void shouldMigrateReviewDraftFromAnonymToAuthUser() {
        Account account = am.create();
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        String body = getResourceAsString(DEFAULT_CAR_REVIEW_DRAFT);

        api.reviews().createReview().subjectPath(AUTO).reqSpec(defaultSpec())
                .reqSpec(r -> {
                    r.setContentType(JSON);
                    r.setBody(body);
                }).xSessionIdHeader(sessionId).xDeviceUidHeader(deviceUid).execute(validatedWith(shouldBeSuccess()));

        AutoApiReviewResponse currentDraft = api.reviews().getCurrentDraft().subjectPath(AUTO).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).xDeviceUidHeader(deviceUid).executeAs(validatedWith(shouldBeSuccess()));

        String authSession = api.auth().login().body(new VertisPassportLoginParameters()
                .login(account.getLogin())
                .password(account.getPassword()))
                .xSessionIdHeader(sessionId)
                .xDeviceUidHeader(deviceUid)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getSession().getId();

        api.reviews().getCurrentDraft().subjectPath(AUTO).reqSpec(defaultSpec())
                .xSessionIdHeader(authSession).xDeviceUidHeader(deviceUid).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));

        prodApi.reviews().getCurrentDraft().subjectPath(AUTO).reqSpec(defaultSpec())
                .xSessionIdHeader(authSession).xDeviceUidHeader(deviceUid).execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));

        AutoApiReviewListingResponse listingResponse = api.userReviews().userReviews().reqSpec(defaultSpec())
                .xSessionIdHeader(authSession).xDeviceUidHeader(deviceUid).executeAs(validatedWith(shouldBeCode(SC_OK)));

        AutoApiReviewListingResponse listingProdResponse = prodApi.userReviews().userReviews().reqSpec(defaultSpec())
                .xSessionIdHeader(authSession).xDeviceUidHeader(deviceUid).executeAs(validatedWith(shouldBeCode(SC_OK)));

        MatcherAssert.assertThat(listingResponse
                .getReviews()
                .stream()
                .map(AutoApiReview::getId)
                .collect(Collectors.toList()),
                hasItem(currentDraft.getReview().getId())
        );
        MatcherAssert.assertThat(listingProdResponse
                .getReviews()
                .stream()
                .map(AutoApiReview::getId)
                .collect(Collectors.toList()),
                hasItem(currentDraft.getReview().getId())
        );
    }
}
