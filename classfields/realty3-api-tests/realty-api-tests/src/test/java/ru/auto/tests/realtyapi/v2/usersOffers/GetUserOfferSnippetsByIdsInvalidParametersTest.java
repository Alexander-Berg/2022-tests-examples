package ru.auto.tests.realtyapi.v2.usersOffers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiVosOffersResponse;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("GET /user/{uid}/offers/byIds")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetUserOfferSnippetsByIdsInvalidParametersTest {
    private static final int MAXIMUM_AMOUNT = 100;
    private RealtyApiVosOffersResponse response;
    private String uid;
    private String token;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Before
    public void createUser() {
        Account account = am.create();
        uid = getUid(account);
        token = oAuth.getToken(account);
        adaptor.vosUser(token);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithNoOfferId() {
        response = api.usersOffers().getSnippetsByIdsRoute().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));

        Assertions.assertThat(response.getError())
                .hasMessage("IllegalArgumentException: requirement failed: Empty offer id");
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeEmptyListWithInvalidIds() {
        response = api.usersOffers().getSnippetsByIdsRoute().reqSpec(authSpec())
                .uidPath(uid)
                .offerIdQuery(getNotExistingOffers(MAXIMUM_AMOUNT))
                .authorizationHeader(token)
                .executeAs(validatedWith(shouldBe200Ok()));

        Assertions.assertThat(response.getResponse()).describedAs("Ответ должен содержать пустой список")
                .hasNoOffers();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithMoreThan100Ids() {
        response = api.usersOffers().getSnippetsByIdsRoute().reqSpec(authSpec())
                .uidPath(uid)
                .offerIdQuery(getNotExistingOffers(MAXIMUM_AMOUNT + 1))
                .authorizationHeader(token)
                .executeAs(validatedWith(shouldBeCode(SC_BAD_REQUEST)));

        Assertions.assertThat(response.getError())
                .hasMessage("requirement failed: Number of offers is more than 100");
    }

    private static Object[] getNotExistingOffers(int amount) {
        return new ArrayList<String >() {{
            for (int i = 0; i < amount; i++) {
                add(getRandomString());
            }
        }}.toArray();
    }
}
