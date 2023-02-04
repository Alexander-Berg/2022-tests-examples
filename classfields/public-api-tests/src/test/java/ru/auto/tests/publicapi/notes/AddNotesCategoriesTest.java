package ru.auto.tests.publicapi.notes;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.objects.NoteRequest;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.USER_ALREADY_HAS_NOTE_ABOUT_THIS_OFFER;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by scrooge on 08.12.17.
 */

@DisplayName("POST /user/notes/{category}/{offerId}")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddNotesCategoriesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(TestData.defaultCategories());
    }

    @Test
    @Owner(SCROOGE)
    public void shouldAddToNotes() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();

        api.userNotes().addNote().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(getRandomString())))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldAddToNotesForAnonym() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();

        String anonymSessionId = adaptor.session().getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();

        api.userNotes().addNote().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(getRandomString())))
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee409AfterTwiceAddToNotes() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();
        String note = Utils.getRandomString();

        api.userNotes().addNote().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(note)))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));

        AutoApiErrorResponse errorResponse = api.userNotes().addNote().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(note)))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_CONFLICT)))
                .as(AutoApiErrorResponse.class);

        assertThat(errorResponse).hasError(USER_ALREADY_HAS_NOTE_ABOUT_THIS_OFFER)
                .hasStatus(ERROR).hasDetailedError(USER_ALREADY_HAS_NOTE_ABOUT_THIS_OFFER.name());
    }
}
