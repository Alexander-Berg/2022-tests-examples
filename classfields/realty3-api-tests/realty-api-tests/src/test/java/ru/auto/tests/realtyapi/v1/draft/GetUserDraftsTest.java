package ru.auto.tests.realtyapi.v1.draft;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
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
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.ME;

@Title("GET /user/{user}/offers/draft")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)

public class GetUserDraftsTest {

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
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.draft().getDraftsForUserRoute()
                .userPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserHasOneDraft() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.createEmptyDraft(token);

        JsonObject response = api.draft().getDraftsForUserRoute().reqSpec(authSpec())
                .userPath(ME)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        String description = "У пользователя должен быть один черновик";

        Assertions.assertThat(response.getAsJsonArray("offers"))
                .describedAs(description)
                .hasSize(1);

        Assertions.assertThat(response.getAsJsonObject("pager").get("totalItems").getAsInt())
                .describedAs(description)
                .isEqualTo(1);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserHasNoDrafts() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        JsonObject response = api.draft().getDraftsForUserRoute().reqSpec(authSpec())
                .userPath(ME)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        String description = "У пользователя не должно быть черновиков";

        Assertions.assertThat(response.getAsJsonArray("offers"))
                .describedAs(description)
                .hasSize(0);

        Assertions.assertThat(response.getAsJsonObject("pager").get("totalItems").getAsInt())
                .describedAs(description)
                .isEqualTo(0);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserCreateDraft() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.createEmptyDraft(token);

        JsonObject response = api.draft().getDraftsForUserRoute().reqSpec(authSpec())
                .userPath(ME)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonArray("offers")
                .get(0).getAsJsonObject();

        String offerId = response.get("id").getAsString();

        String offerStatus = api.userOffers().getUserOfferRoute().reqSpec(authSpec())
                .offerIdPath(offerId)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .get("offerStatus")
                .getAsString();

        Assertions.assertThat(response.get("status").getAsString())
                .describedAs("Созданный оффер должен быть черновиком")
                .isEqualTo(offerStatus);
    }
}
