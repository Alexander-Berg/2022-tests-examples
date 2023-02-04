package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplate;
import ru.auto.tests.publicapi.model.AutoApiDraftResponse;
import ru.auto.tests.publicapi.model.VertisPassportLoginParameters;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiLogoutResponse;
import ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.consts.Owners.SCROOGE;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.TestData.defaultOffersByBaseCategories;

/**
 * Created by scrooge on 10.11.17.
 */

@DisplayName("MIGRATE draft from anonym to auth user")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Issue("VERTISTEST-624")
public class DraftMigrateTest {

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
    public CategoryEnum category;

    @Parameter("Оффер")
    @Parameterized.Parameter(1)
    public String offer;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0} - {1}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(defaultOffersByBaseCategories());
    }

    @Test
    @Owner(SCROOGE)
    @Description("Создаем драфт анонима. Логинимся на новый акк (на котором отсутствует драфт). " +
            "Открывается драфт анонима")
    public void shouldMigrateDraftFromAnonymToAuthUser() {
        VertisPassportSession session = adaptor.session().getSession();
        String sessionId = session.getId();
        String deviceUid = session.getDeviceUid();

        String body = new OfferTemplate().process(offer, account.getLogin());

        AutoApiDraftResponse response = api.draft().currentDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .reqSpec(r -> {
                    r.setContentType(JSON);
                    r.setBody(body);
                }).xSessionIdHeader(sessionId).xDeviceUidHeader(deviceUid)
                .executeAs(validatedWith((shouldBeSuccess())));

        String authSession = api.auth().login().body(new VertisPassportLoginParameters()
                .login(account.getLogin())
                .password(account.getPassword()))
                .xSessionIdHeader(sessionId)
                .xDeviceUidHeader(deviceUid)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getSession().getId();

        AutoApiDraftResponse authResponse = api.draft().currentDraft().categoryPath(category.name())
                .xSessionIdHeader(authSession).xDeviceUidHeader(deviceUid).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        assertThat(authResponse).hasOfferId(response.getOfferId());
    }

    @Test
    @Owner(SCROOGE)
    @Description("Логинимся на акк, создаем драфт, разлогиниваемся. Создаем драфт анонима. " +
            "Логинимся на предыдущий акк - открывает акк созданный залогином ранее (а не драфт анонима)")
    public void shouldMigrateDraftFromAuthToAnonymUser() {
        AutoApiLoginResponse userAuth = adaptor.login(account);
        String sessionIdAuth = userAuth.getSession().getId();
        String deviceUid = userAuth.getSession().getDeviceUid();

        String bodyAuth = new OfferTemplate().process(offer, account.getLogin());
        AutoApiDraftResponse responseAuth = api.draft().currentDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .reqSpec(r -> {
                    r.setContentType(JSON);
                    r.setBody(bodyAuth);
                }).xSessionIdHeader(sessionIdAuth)
                .xDeviceUidHeader(deviceUid)
                .executeAs(validatedWith((shouldBeSuccess())));

        AutoApiLogoutResponse responseLogout = api.auth().logout().reqSpec(defaultSpec()).xSessionIdHeader(sessionIdAuth)
                .xDeviceUidHeader(userAuth.getSession().getDeviceUid())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        String sessionIdAfterLogout = responseLogout.getSession().getId();

        String bodyAfterLogout = new OfferTemplate().process(offer, account.getLogin());
        api.draft().currentDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .reqSpec(r -> {
                    r.setContentType(JSON);
                    r.setBody(bodyAfterLogout);
                }).xSessionIdHeader(sessionIdAfterLogout)
                .xDeviceUidHeader(deviceUid)
                .execute(validatedWith((shouldBeSuccess())));

        String authSession = api.auth().login().body(new VertisPassportLoginParameters()
                .login(account.getLogin())
                .password(account.getPassword()))
                .xSessionIdHeader(sessionIdAfterLogout)
                .xDeviceUidHeader(deviceUid)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getSession().getId();

        AutoApiDraftResponse authResponse2 = api.draft().currentDraft().categoryPath(category.name())
                .xSessionIdHeader(authSession)
                .xDeviceUidHeader(deviceUid).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        assertThat(authResponse2).hasOfferId(responseAuth.getOfferId());
    }

    @Test
    @Issue("AUTORUAPI-4346")
    @Owner(DSKUZNETSOV)
    @Description("Логинимся на акк, создаем драфт, разлогиниваемся. Создаем драфт анонима. " +
            "Логинимся на предыдущий акк, удаляем драфт - должен открыться пустой драфт")
    public void shouldNotMigrateDraftFromAnonymToAuthUserWithDraft() {
        AutoApiLoginResponse userAuth = adaptor.login(account);
        String sessionIdAuth = userAuth.getSession().getId();
        String deviceUid = userAuth.getSession().getDeviceUid();

        String body = new OfferTemplate().process(offer, account.getLogin());

        String authEmptyDraftId = api.draft().currentDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionIdAuth)
                .xDeviceUidHeader(deviceUid)
                .executeAs(validatedWith((shouldBeSuccess()))).getOfferId();

        api.draft().updateDraft().offerIdPath(authEmptyDraftId).categoryPath(category.name()).reqSpec(defaultSpec())
                .reqSpec(r -> {
                    r.setContentType(JSON);
                    r.setBody(body);
                }).xSessionIdHeader(sessionIdAuth)
                .xDeviceUidHeader(deviceUid)
                .executeAs(validatedWith((shouldBeSuccess())));

        String sessionIdAfterLogout = api.auth().logout().reqSpec(defaultSpec()).xSessionIdHeader(sessionIdAuth)
                .xDeviceUidHeader(userAuth.getSession().getDeviceUid())
                .executeAs(validatedWith(shouldBe200OkJSON())).getSession().getId();

        String anonDraftId = api.draft().currentDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionIdAfterLogout)
                .xDeviceUidHeader(deviceUid)
                .executeAs(validatedWith((shouldBeSuccess()))).getOfferId();

        api.draft().updateDraft().offerIdPath(anonDraftId).categoryPath(category.name()).reqSpec(defaultSpec())
                .reqSpec(r -> {
                    r.setContentType(JSON);
                    r.setBody(body);
                }).xSessionIdHeader(sessionIdAfterLogout)
                .xDeviceUidHeader(deviceUid)
                .execute(validatedWith((shouldBeSuccess())));

        String authSession = api.auth().login().body(new VertisPassportLoginParameters()
                .login(account.getLogin())
                .password(account.getPassword()))
                .xSessionIdHeader(sessionIdAfterLogout)
                .xDeviceUidHeader(deviceUid)
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getSession().getId();

        api.draft().currentDraft().categoryPath(category.name())
                .xSessionIdHeader(authSession)
                .xDeviceUidHeader(deviceUid).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()));

        api.draft().deleteDraft().offerIdPath(authEmptyDraftId).categoryPath(category.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(authSession)
                .xDeviceUidHeader(deviceUid)
                .execute(validatedWith((shouldBeSuccess())));

        String draftIdAfterDelete = api.draft().currentDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionIdAuth)
                .xDeviceUidHeader(deviceUid)
                .executeAs(validatedWith((shouldBeSuccess()))).getOfferId();

        MatcherAssert.assertThat("Получили черновик анонима после удаления черновика залогина", draftIdAfterDelete, Matchers.not(anonDraftId));
    }
}