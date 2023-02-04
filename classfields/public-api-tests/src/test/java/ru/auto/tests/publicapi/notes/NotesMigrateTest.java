package ru.auto.tests.publicapi.notes;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiOfferListingResponse;
import ru.auto.tests.publicapi.model.VertisPassportLoginParameters;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.objects.NoteRequest;
import ru.auto.tests.publicapi.testdata.TestData;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by scrooge on 12.12.17.
 */

@DisplayName("MIGRATE notes from anonym to auth user")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Issue("VERTISTEST-624")
public class NotesMigrateTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private Account account;

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
    @Description("Проверяем, что после авторизации, заметки переносятся авторизованному пользователю")
    public void shouldMigrateNotesFromAnonymToAuthUser() {
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = adaptor.login(account).getSession().getId();
        String anonymSessionId = session.getId();
        String deviceUid = session.getDeviceUid();
        String offerId = adaptor.createOffer(account.getLogin(), sessionId, category).getOfferId();

        api.userNotes().addNote().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(Utils.getRandomString())))
                .reqSpec(defaultSpec()).xSessionIdHeader(anonymSessionId)
                .xDeviceUidHeader(deviceUid)
                .execute(validatedWith(shouldBeSuccess()));

        String authSession = api.auth().login().body(new VertisPassportLoginParameters()
                .login(account.getLogin())
                .password(account.getPassword()))
                .xSessionIdHeader(anonymSessionId)
                .xDeviceUidHeader(deviceUid)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getSession().getId();

        AutoApiOfferListingResponse response = api.userNotes().notes().categoryPath(category.name())
                .reqSpec(defaultSpec()).xSessionIdHeader(authSession).xDeviceUidHeader(deviceUid)
                .executeAs(validatedWith(shouldBeSuccess()));

        MatcherAssert.assertThat(String.format("Не перенесли оффер %s из заметок при авторизации", offerId),
                response.getOffers(), hasSize(1));
        AutoruApiModelsAssertions.assertThat(response.getOffers().get(0)).hasCategory(category).hasId(offerId);
    }


}
