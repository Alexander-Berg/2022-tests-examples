package ru.auto.tests.realtyapi.v1.common;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.enums.ComplainReason;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.IdResponse;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.OldComplain;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.enums.ComplainReason.ANOTHER;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;


@Title("POST /complain.json")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class ComplainTest {

    //partnerId - это из capa (для vos это фиксированное число 1035218734)
    private static final String PARTNER_ID = "1035218734";

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

    @Test
    public void shouldSee403WithoutHeaders() {
        api.common().complainRoute().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldComplaintWithoutOAuth() {
        Account account = am.create();
        api.common().complainRoute().reqSpec(authSpec()).body(getComplain(account.getId()))
                .execute(validatedWith(shouldBeOK()));
    }

    @Test
    public void shouldSee400WithoutBody() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.common().complainRoute().reqSpec(authSpec()).authorizationHeader(token).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSeeAnotherId() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.getOfferIdFromSearcher();
        IdResponse response = api.common().complainRoute().reqSpec(authSpec())
                .body(new OldComplain().entityId(offerId)
                        .partnerId(PARTNER_ID)
                        .reason(ANOTHER.name())
                        .uid(account.getId())
                        .text(Utils.getRandomString()))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeOK())).as(IdResponse.class, GSON);
        IdResponse secondResponse = api.common().complainRoute().reqSpec(authSpec())
                .body(new OldComplain().entityId(offerId)
                        .partnerId(PARTNER_ID)
                        .reason(ANOTHER.name())
                        .uid(account.getId())
                        .text(Utils.getRandomString()))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeOK())).as(IdResponse.class, GSON);

        Assertions.assertThat(secondResponse.getResponse().getId())
                .isNotEqualTo(response.getResponse().getId());
    }

    private OldComplain getComplain(String uid) {
        return new OldComplain().entityId(adaptor.getOfferIdFromSearcher())
                .partnerId(PARTNER_ID)
                .reason(ANOTHER.toString())
                .uid(uid)
                .text(Utils.getRandomString());
    }
}
