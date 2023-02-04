package ru.auto.tests.publicapi.user.phones;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.ResponseSpecBuilders;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.VertisPassportAddPhoneParameters;
import ru.auto.tests.publicapi.model.VertisPassportConfirmPhoneParameters;
import ru.auto.tests.publicapi.model.VertisPassportLoginOrRegisterParameters;
import ru.auto.tests.publicapi.module.PublicApiModule;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by dskuznetsov on 22.08.18
 */


@DisplayName("POST /user/phones/confirm")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class ConfirmPhoneTest {

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.userPhones().confirmPhone().body(new VertisPassportConfirmPhoneParameters().code(Utils.getRandomString()).phone(Utils.getRandomPhone()))
                .executeAs(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldNotConfirmTwiceWithOneCode() {
        Account account = am.create();
        String code = getSmsCodeAfterLoginOrRegister(account);

        api.userPhones().confirmPhone().body(new VertisPassportConfirmPhoneParameters().code(code).phone(account.getPhone().toString())).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        api.userPhones().confirmPhone().body(new VertisPassportConfirmPhoneParameters().code(code).phone(account.getPhone().toString())).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldConfirmPhoneAfterLoginOrRegister() {
        Account account = am.create();
        String code = getSmsCodeAfterLoginOrRegister(account);

        api.userPhones().confirmPhone().body(new VertisPassportConfirmPhoneParameters().code(code).phone(account.getPhone().toString())).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Test
    public void shouldConfirmPhoneAfterAddPhone() {
        Account account = am.create();
        String code = getSmsCodeAfterAddPhone(account);

        api.userPhones().confirmPhone().body(new VertisPassportConfirmPhoneParameters().code(code).phone(account.getPhone().toString())).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Test
    public void shouldSeeNoDiffWithProduction() {
        Account account = am.create();
        String code = getSmsCodeAfterAddPhone(account);
        String secondCode = getSecondSmsCodeAfterAddPhone(account);

        JsonObject response = api.userPhones().confirmPhone()
                .body(new VertisPassportConfirmPhoneParameters().code(code).phone(account.getPhone().toString())).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);

        JsonObject responseProd = prodApi.userPhones().confirmPhone()
                .body(new VertisPassportConfirmPhoneParameters().code(secondCode).phone(account.getPhone().toString())).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class);

        MatcherAssert.assertThat(response, jsonEquals(responseProd).whenIgnoringPaths("session.id", "session.expire_timestamp", "session.device_uid", "session.creation_timestamp", "userTicket"));
    }

    @Step("Получаем смс код после логина пользователем {account.login}:{account.password} ({account.id})")
    public String getSmsCodeAfterLoginOrRegister(Account account) {
        api.auth().loginOrRegister().body(new VertisPassportLoginOrRegisterParameters().phone(account.getPhone().toString()))
                .reqSpec(defaultSpec()).executeAs(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));

        return adaptor.getSmsCode(account.getId(), 0);
    }

    @Step("Получаем смс код после добавления нового телефона пользователю {account.login}:{account.password} ({account.id})")
    public String getSmsCodeAfterAddPhone(Account account) {
        api.userPhones().addPhone().body(new VertisPassportAddPhoneParameters().phone(account.getPhone().toString()))
                .reqSpec(defaultSpec()).executeAs(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));

        return adaptor.getSmsCode(account.getId(), 0);
    }

    @Step("Получаем второй смс код после добавления нового телефона пользователю {account.login}:{account.password} ({account.id})")
    public String getSecondSmsCodeAfterAddPhone(Account account) {
        api.userPhones().addPhone().body(new VertisPassportAddPhoneParameters().phone(account.getPhone().toString()))
                .reqSpec(defaultSpec()).executeAs(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));
        return adaptor.getSmsCode(account.getId(), 1);
    }
}