package ru.auto.tests.publicapi.carfax;

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
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpStatus.*;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.CARFAX;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("PUT /carfax/user/comment")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class CreateCommentTest {

    private static final String OFFER_VIN = "X9FLXXEEBLES67719";
    private static final String WITHOUT_OFFER_VIN = "SALWA2FK7HA135034";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager accountManager;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private ApiClient api;

    @Test
    @Owner(TIMONDL)
    public void shouldSuccessCreateNewComment() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String commentText = getRandomString(100);
        String blockId = getRandomString();
        AutoApiVinCommentsAddVinReportCommentRequest body = new AutoApiVinCommentsAddVinReportCommentRequest()
                .comment(new AutoApiVinCommentsVinReportComment().blockId(blockId).text(commentText));

        String offerId = RawReportUtils.createTestOffer(adaptor, account, sessionId);

        adaptor.waitUserOffersActive(sessionId, CARS, OFFER_VIN);

        AutoApiVinCommentsVinReportCommentAddResponse response = api.carfax().putComment().reqSpec(defaultSpec())
                .vinQuery(OFFER_VIN)
                .body(body)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        AutoApiVinCommentsVinReportCommentAssert.assertThat(response.getComment())
                .hasBlockId(blockId).hasText(commentText);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSuccessUpdateComment() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = RawReportUtils.createTestOffer(adaptor, account, sessionId);

        String newCommentText = getRandomString(100);
        String blockId = getRandomString();
        adaptor.waitUserOffersActive(sessionId, CARS, OFFER_VIN);

        adaptor.createCarfaxComment(sessionId, OFFER_VIN, blockId, getRandomString(100));

        AutoApiVinCommentsAddVinReportCommentRequest body = new AutoApiVinCommentsAddVinReportCommentRequest()
                .comment(new AutoApiVinCommentsVinReportComment().blockId(blockId).text(newCommentText));

        AutoApiVinCommentsVinReportCommentAddResponse response = api.carfax().putComment().reqSpec(defaultSpec())
                .vinQuery(OFFER_VIN)
                .body(body)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        AutoApiVinCommentsVinReportCommentAssert.assertThat(response.getComment())
                .hasBlockId(blockId).hasText(newCommentText);
    }

    @Test
    @Owner(TIMONDL)
    public void cantCreateNewComment() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String commentText = getRandomString(100);
        String blockId = getRandomString();
        AutoApiVinCommentsAddVinReportCommentRequest body = new AutoApiVinCommentsAddVinReportCommentRequest()
                .comment(new AutoApiVinCommentsVinReportComment().blockId(blockId).text(commentText));

        api.carfax().putComment().reqSpec(defaultSpec())
                .vinQuery(WITHOUT_OFFER_VIN)
                .body(body)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(CARFAX)
    public void shouldNotCreateNewCommentForForeignOffer() {
        Account accountOne = accountManager.create();
        String sessionId = adaptor.login(accountOne).getSession().getId();

        String offerId = RawReportUtils.createTestOffer(adaptor, accountOne, sessionId);

        adaptor.waitUserOffersActive(sessionId, CARS, OFFER_VIN);

        Account accountTwo = accountManager.create();
        String sessionIdTwo = adaptor.login(accountTwo).getSession().getId();
        String commentText = getRandomString(100);
        String blockId = getRandomString();
        AutoApiVinCommentsAddVinReportCommentRequest body = new AutoApiVinCommentsAddVinReportCommentRequest()
                .comment(new AutoApiVinCommentsVinReportComment().blockId(blockId).text(commentText));

        api.carfax().putComment().reqSpec(defaultSpec())
                .vinQuery(OFFER_VIN)
                .body(body)
                .xSessionIdHeader(sessionIdTwo)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(CARFAX)
    public void cantCreateCommentWithoutSession() {
        Account accountOne = accountManager.create();
        String sessionId = adaptor.login(accountOne).getSession().getId();

        String offerId = RawReportUtils.createTestOffer(adaptor, accountOne, sessionId);

        adaptor.waitUserOffersActive(sessionId, CARS, OFFER_VIN);

        String commentText = getRandomString(100);
        String blockId = getRandomString();
        AutoApiVinCommentsAddVinReportCommentRequest body = new AutoApiVinCommentsAddVinReportCommentRequest()
                .comment(new AutoApiVinCommentsVinReportComment().blockId(blockId).text(commentText));

        api.carfax().putComment().reqSpec(defaultSpec())
                .vinQuery(OFFER_VIN)
                .body(body)
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(CARFAX)
    public void cantCreateTooShortComment() {
        Account accountOne = accountManager.create();
        String sessionId = adaptor.login(accountOne).getSession().getId();

        String offerId = RawReportUtils.createTestOffer(adaptor, accountOne, sessionId);

        adaptor.waitUserOffersActive(sessionId, CARS, OFFER_VIN);

        String commentText = getRandomString(1);
        String blockId = getRandomString();
        AutoApiVinCommentsAddVinReportCommentRequest body = new AutoApiVinCommentsAddVinReportCommentRequest()
                .comment(new AutoApiVinCommentsVinReportComment().blockId(blockId).text(commentText));

        api.carfax().putComment().reqSpec(defaultSpec())
                .vinQuery(OFFER_VIN)
                .body(body)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_UNPROCESSABLE_ENTITY)));
    }

    @Test
    @Owner(CARFAX)
    public void cantCreateTooLongComment() {
        Account accountOne = accountManager.create();
        String sessionId = adaptor.login(accountOne).getSession().getId();

        String offerId = RawReportUtils.createTestOffer(adaptor, accountOne, sessionId);

        adaptor.waitUserOffersActive(sessionId, CARS, OFFER_VIN);

        String commentText = getRandomString(99999);
        String blockId = getRandomString();
        AutoApiVinCommentsAddVinReportCommentRequest body = new AutoApiVinCommentsAddVinReportCommentRequest()
                .comment(new AutoApiVinCommentsVinReportComment().blockId(blockId).text(commentText));

        api.carfax().putComment().reqSpec(defaultSpec())
                .vinQuery(OFFER_VIN)
                .body(body)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_UNPROCESSABLE_ENTITY)));
    }

    @Test
    @Owner(CARFAX)
    public void cantCreateWithTooMuchPhotosComment() {
        Account accountOne = accountManager.create();
        String sessionId = adaptor.login(accountOne).getSession().getId();

        String offerId = RawReportUtils.createTestOffer(adaptor, accountOne, sessionId);

        adaptor.waitUserOffersActive(sessionId, CARS, OFFER_VIN);

        List<AutoApiVinPhoto> photos = new ArrayList<>();

        for (int i = 1; i < 15; i++) {
            AutoApiVinPhoto photo =
                    new AutoApiVinPhoto()
                            .mdsPhotoInfo(new AutoApiInternalMdsPhotoInfo()
                                    .groupId(i)
                                    .name(i + "name")
                                    .namespace(i + "namespace"));
            photos.add(photo);
        }

        String commentText = getRandomString(100);
        String blockId = getRandomString();
        AutoApiVinCommentsAddVinReportCommentRequest body = new AutoApiVinCommentsAddVinReportCommentRequest()
                .comment(new AutoApiVinCommentsVinReportComment().blockId(blockId).text(commentText).photos(photos));

        api.carfax().putComment().reqSpec(defaultSpec())
                .vinQuery(OFFER_VIN)
                .body(body)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_UNPROCESSABLE_ENTITY)));
    }

}
