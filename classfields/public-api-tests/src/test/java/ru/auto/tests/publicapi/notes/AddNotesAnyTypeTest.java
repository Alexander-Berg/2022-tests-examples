package ru.auto.tests.publicapi.notes;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.objects.NoteRequest;
import ru.yandex.qatools.allure.annotations.Parameter;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("POST /user/notes/{category}/{offerId}")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddNotesAnyTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Parameter
    @Parameterized.Parameter(0)
    public String note;

    @Parameterized.Parameters(name = "note={0}")
    public static String[] provideNotes() {
        return new String[]{
                getRandomString(),
                getRandomString(100),
        };
    }

    @Test
    @Owner(SCROOGE)
    public void shouldAddAnyTypeOfNotes() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String anonymSessionId = adaptor.session().getSession().getId();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, CARS).getOfferId();

        api.userNotes().addNote().categoryPath(CARS.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(note)))
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }
}
