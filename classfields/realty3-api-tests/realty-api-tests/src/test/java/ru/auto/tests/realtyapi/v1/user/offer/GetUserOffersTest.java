package ru.auto.tests.realtyapi.v1.user.offer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.UserOffersResp;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.assertj.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomUID;


@Title("GET /user/{uid}/offers")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetUserOffersTest {

    private static final long DEFAULT_PAGE_SIZE = 10L;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Inject
    private OAuth oAuth;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.userOffers().getAnyUserOffersRoute().uidPath(getRandomUID())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee200AndEmptyForNotVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        UserOffersResp resp = api.userOffers().getAnyUserOffersRoute().reqSpec(authSpec()).authorizationHeader(token)
                .uidPath(account.getId())
                .execute(validatedWith(shouldBeOK())).as(UserOffersResp.class, GSON);

        assertThat(resp.getResponse()).hasNoOffers();
        assertThat(resp.getResponse().getPager()).hasPage(1L).hasPageSize(DEFAULT_PAGE_SIZE)
                .hasTotalPages(1L).hasTotalItems(0L);
    }

    @Test
    public void shouldGetOffer() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String id = adaptor.createOffer(token).getResponse().getId();

        UserOffersResp resp = api.userOffers().getAnyUserOffersRoute().reqSpec(authSpec()).authorizationHeader(token)
                .uidPath(account.getId())
                .execute(validatedWith(shouldBeOK())).as(UserOffersResp.class, GSON);

        assertThat(resp.getResponse().getOffers()).hasSize(1);
        assertThat(resp.getResponse().getOffers().get(0)).hasId(id);
        assertThat(resp.getResponse().getPager()).hasPage(1L).hasPageSize(DEFAULT_PAGE_SIZE)
                .hasTotalPages(1L).hasTotalItems(1L);
    }
}
