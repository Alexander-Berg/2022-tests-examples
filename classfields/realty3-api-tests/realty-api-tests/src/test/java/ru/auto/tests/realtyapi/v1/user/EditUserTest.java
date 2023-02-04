package ru.auto.tests.realtyapi.v1.user;

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
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.UserResponse;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.ExamplePhoneModel;
import ru.auto.tests.realtyapi.v1.model.ExampleUserInfoModel;
import ru.auto.tests.realtyapi.v1.model.ExampleUserModel;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("PATCH /user")
@Description("Инициализация пользователя в VOS")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class EditUserTest {

    private static final String RU_LOCALE = "ru_RU";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.user().patchUserRoute().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithoutOAuth() {
        api.user().patchUserRoute().reqSpec(authSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.user().patchUserRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldEditUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        UserResponse userResp = adaptor.getUser(token);
        String phoneId = userResp.getResponse().getUser().getPhones().get(0).getId();
        String phone = userResp.getResponse().getUser().getPhones().get(0).getPhone();
        String name = Utils.getRandomString();
        String email = Utils.getRandomEmail();
        boolean select = true;

        UserResponse response = api.user().patchUserRoute().reqSpec(authSpec())
                .body(new ExampleUserModel().user(new ExampleUserInfoModel().name(name).email(email)
                        .phones(newArrayList(new ExamplePhoneModel()
                                .phone(account.getPhone().get()).select(select).id(phoneId))))
                        .locale(RU_LOCALE))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok())).as(UserResponse.class, GSON);

        assertThat(response.getResponse()).hasValid(true);
        assertThat(response.getResponse().getUser()).hasName(name).hasEmail(email);
        Assertions.assertThat(response.getResponse().getUser().getPhones()).hasSize(1);
        assertThat(response.getResponse().getUser().getPhones().get(0))
                .hasId(phoneId).hasSelect(select).hasPhone(phone);
    }
}
