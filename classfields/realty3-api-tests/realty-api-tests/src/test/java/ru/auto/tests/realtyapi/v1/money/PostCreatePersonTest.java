package ru.auto.tests.realtyapi.v1.money;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.Gson;
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
import ru.auto.tests.realtyapi.v1.model.BootstrapRequest;
import ru.auto.tests.realtyapi.v1.model.UrPerson;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getEmptyBody;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("POST /money/person/{uid}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class PostCreatePersonTest {

    private static final String CLIENT_ID = "31500762";

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
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.money().createPerson()
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithInvalidUid() {
        api.money().createPerson().reqSpec(authSpec())
                .uidPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithNoBody() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        api.money().createPerson().reqSpec(authSpec())
                .reqSpec(r -> r.setBody(getEmptyBody()))
                .uidPath(account.getId())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeClientId() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.juridicalUser(account, token);

        api.money().bootstrapBilling().reqSpec(authSpec())
                .uidPath(account.getId())
                .body(getBootstrapRequestForUser(account))
                .execute(validatedWith(shouldBe200Ok()));

        JsonObject response = api.money().createPerson().reqSpec(authSpec())
                .uidPath(account.getId())
                .body(getPostBody())
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.get("clientId").isJsonNull())
                .describedAs("\"clientId\" = null").isTrue();
        Assertions.assertThat(response.get("personId"))
                .describedAs("personId должен присутствовать").isNotNull();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserHasPersonId() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.juridicalUser(account, token);
        String uid = account.getId();

        api.money().bootstrapBilling().reqSpec(authSpec())
                .uidPath(uid)
                .body(getBootstrapRequestForUser(account))
                .execute(validatedWith(shouldBe200Ok()));

        JsonObject createResponse = api.money().createPerson().reqSpec(authSpec())
                .uidPath(uid)
                .body(getPostBody())
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        JsonObject findResponse = api.money().findPersons().reqSpec(authSpec())
                .uidPath(uid)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonArray("persons")
                .get(0).getAsJsonObject();

        Assertions.assertThat(createResponse.get("personId").getAsString())
                .describedAs("Есть person в балансе с personId")
                .isEqualTo(findResponse.get("id").getAsString());
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldEveryFieldIsSaved() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.juridicalUser(account, token);
        String uid = account.getId();

        UrPerson postBody = getPostBody();

        api.money().bootstrapBilling().reqSpec(authSpec())
                .uidPath(uid)
                .body(getBootstrapRequestForUser(account))
                .execute(validatedWith(shouldBe200Ok()));

        api.money().createPerson().reqSpec(authSpec())
                .uidPath(uid)
                .body(postBody)
                .execute(validatedWith(shouldBe200Ok()));

        JsonObject person = api.money().findPersons().reqSpec(authSpec())
                .uidPath(uid)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonArray("persons")
                .get(0).getAsJsonObject();

        Gson gson = new Gson();
        //sync request and response formats
        person.addProperty("email", person.getAsJsonArray("emails").get(0).getAsString());
        UrPerson urPerson = gson.fromJson(person, UrPerson.class);
        Assertions.assertThat(urPerson).describedAs("Поля при создании указаны в выдаче")
                .isEqualTo(postBody);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee404WithClientId() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);
        adaptor.juridicalUser(account, token);
        String uid = account.getId();

        api.money().findPersons().reqSpec(authSpec())
                .uidPath(uid)
                .clientIdQuery(CLIENT_ID)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    public static UrPerson getPostBody() {
        String name = "ООО \"ПРОСТОР-РИЭЛТИ\"";
        String address = "Санкт-Петербург, ул. Бутлерова д. 9";
        return new UrPerson()
                .name(name)
                .longname(name)
                .phone("+7 (495) 921-21-80. доб. 06005")
                .email("petrova@naprostore.ru")
                .legaladdress(address)
                .postaddress(address)
                .inn("6449013711")
                .kpp("644901001")
                .signerPersonName("Бори Борисович Борисов")
                .postcode("190068")
                .representative("representative name");
    }

    public static BootstrapRequest getBootstrapRequestForUser(Account account) {
        return new BootstrapRequest()
                .name(account.getName())
                .adAgency(false);
    }
}
