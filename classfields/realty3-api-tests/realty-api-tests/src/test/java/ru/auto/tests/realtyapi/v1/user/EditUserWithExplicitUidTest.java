package ru.auto.tests.realtyapi.v1.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.UpdateExplicitUserResp;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.VosUserModel;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.Lists.newArrayList;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe400WithInvalidEmptyParams;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getEmptyBody;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomUID;
import static ru.auto.tests.realtyapi.v1.model.VosUserModel.PaymentTypeEnum.NATURAL_PERSON;


@Title("PUT /user/{uid}")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class EditUserWithExplicitUidTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.user().updateUserRoute().uidPath(getRandomUID()).body(getBodyRequest(getRandomUID()))
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithoutBody() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.user().updateUserRoute().reqSpec(authSpec()).authorizationHeader(token)
                .uidPath(account.getId()).reqSpec(jsonBody(StringUtils.EMPTY))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.user().updateUserRoute().reqSpec(authSpec()).authorizationHeader(token)
                .uidPath(account.getId()).reqSpec(jsonBody(getEmptyBody()))
                .execute(validatedWith(shouldBe400WithInvalidEmptyParams()));
    }

    @Test
    public void shouldEditUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        VosUserModel reqBody = getBodyRequest(account.getId());

        UpdateExplicitUserResp resp = api.user().updateUserRoute().reqSpec(authSpec()).authorizationHeader(token)
                .uidPath(account.getId()).body(reqBody)
                .execute(validatedWith(shouldBe200OkJSON())).as(UpdateExplicitUserResp.class, GSON);

        Assertions.assertThat(resp).hasLogin(reqBody.getLogin())
                .hasType(reqBody.getType().longValue())
                .hasEmail(reqBody.getEmail())
                .hasName(reqBody.getName())
                .hasOrganization(reqBody.getOrganization())
                .hasOgrn(reqBody.getOgrn())
                .hasAgencyId(reqBody.getAgencyId())
                .hasCallCenter(reqBody.getCallCenter())
                .hasPhones(reqBody.getTelephones().stream().toArray(String[]::new))
                .hasPhotoUrl(reqBody.getPhotoUrl())
                .hasLicenseAgreement(reqBody.getLicenseAgreement())
                .hasPaymentType(reqBody.getPaymentType().getValue());
    }

    private VosUserModel getBodyRequest(String uid) {
        return random(VosUserModel.class).login(uid).type(0).email("test@yandex.q")
                .paymentType(NATURAL_PERSON)
                .allowedCommunicationChannels(newArrayList("COM_CALLS"));
    }
}
