package ru.auto.tests.publicapi.notes;

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
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.objects.NoteRequest;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400IncorrectOfferIdError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


/**
 * Created by scrooge on 27.12.17.
 */


@DisplayName("PUT /user/notes/{category}/{offerId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class ForceAddNotesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    @Owner(SCROOGE)
    public void shouldSee403WhenNoAuth() {
        api.userNotes().upsertNote().categoryPath(CARS.name()).offerIdPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee400WithIncorrectCategory() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        String incorrectCategory = getRandomString();
        api.userNotes().upsertNote().categoryPath(incorrectCategory).offerIdPath(offerId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400UnknownCategoryError(incorrectCategory)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSeeSuccessWithoutSessionId() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();
        api.userNotes().upsertNote().categoryPath(CARS.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(getRandomString())))
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee400WithIncorrectOfferId() {
        String incorrectOfferId = getRandomString();
        api.userNotes().upsertNote().categoryPath(CARS.name()).offerIdPath(incorrectOfferId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400IncorrectOfferIdError(incorrectOfferId)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldTwiceAddToNotes() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        String note = getRandomString();

        api.userNotes().upsertNote().categoryPath(CARS.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(note)))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));

        api.userNotes().upsertNote().categoryPath(CARS.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(note)))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee400WithWrongOfferId() {
        String incorrectOfferId = getRandomString();
        api.userNotes().upsertNote().categoryPath(CARS.name()).offerIdPath(incorrectOfferId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400IncorrectOfferIdError(incorrectOfferId)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee400WithoutText() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String anonymSessionId = adaptor.session().getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        api.userNotes().upsertNote().categoryPath(CARS.name()).offerIdPath(offerId)
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }
}
