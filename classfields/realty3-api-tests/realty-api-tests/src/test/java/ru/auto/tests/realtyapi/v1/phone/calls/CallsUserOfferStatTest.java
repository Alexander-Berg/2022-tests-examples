package ru.auto.tests.realtyapi.v1.phone.calls;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
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
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomOfferId;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomUID;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.GetDate.getFurtherFutureTime;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.GetDate.getNearFutureTime;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.INVALID_PAGE;


@Title("GET /phone/calls/user/{uid}/{offerId}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class CallsUserOfferStatTest {

    private Account account;
    private String offerId;

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
        account = am.create();
        String token = oAuth.getToken(account);
        offerId = adaptor.createOffer(token).getResponse().getId();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.phoneCalls().callsForUserOfferRoute()
                .uidPath(getRandomUID())
                .offerIdPath(getRandomOfferId())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee404ForInvalidOffer() {
        api.phoneCalls().callsForUserOfferRoute()
                .uidPath(account.getId())
                .offerIdPath(getRandomOfferId())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeEmptyStatsForNewOffer() {
        JsonObject response = api.phoneCalls().callsForUserOfferRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .offerIdPath(offerId)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.getAsJsonArray("calls"))
                .describedAs("У нового оффера не должно быть звонков")
                .isEmpty();

        Assertions.assertThat(response.getAsJsonObject("pager").get("totalItems").getAsInt())
                .describedAs("У нового оффера не должно быть звонков")
                .isZero();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidPage() {
        api.phoneCalls().callsForUserOfferRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .offerIdPath(offerId)
                .pageQuery(INVALID_PAGE)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidPageSize() {
        api.phoneCalls().callsForUserOfferRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .offerIdPath(offerId)
                .pageSizeQuery(INVALID_PAGE)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee200WithStartDateInFuture() {
        api.phoneCalls().callsForUserOfferRoute().reqSpec(authSpec())
                .uidPath(account.getId())
                .offerIdPath(offerId)
                .startTimeQuery(getNearFutureTime())
                .endTimeQuery(getFurtherFutureTime())
                .execute(validatedWith(shouldBe200Ok()));
    }
}
