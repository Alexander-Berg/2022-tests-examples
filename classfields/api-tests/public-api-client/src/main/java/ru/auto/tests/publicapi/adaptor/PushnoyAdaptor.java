package ru.auto.tests.publicapi.adaptor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.codec.digest.DigestUtils;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.JSON;
import ru.auto.tests.publicapi.ResponseSpecBuilders;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.config.PublicApiConfig;
import ru.auto.tests.publicapi.model.AutoApiDevice;
import ru.auto.tests.publicapi.model.AutoApiHelloRequest;
import ru.auto.tests.publicapi.model.VertisPassportLoginOptions;
import ru.auto.tests.publicapi.model.VertisPassportLoginParameters;

import static java.lang.String.format;
import static java.lang.System.getProperties;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@Singleton
public class PushnoyAdaptor extends PublicApiAdaptor {

    @Inject
    @Prod
    private ApiClient api;

    @Inject
    private PublicApiConfig config;

    @Step("Получаем список девайсов привязанном в пушном для юзера {userType}:{userId}")
    public String getDeviceList(String userType, String userId) {
        return RestAssured.given().filter(new AllureLoggerFilter())
                .baseUri(config.getPushnoyApiURI().toString())
                .header("Accept", "application/json")
                .contentType("application/json")
                .pathParam("app","auto")
                .pathParam("userId", format("%s:%s", userType, userId))
                .when()
                .get("/{app}/user/{userId}")
                .getBody().asString();
    }

    @Step("Удаляем связку девайс-юзер в пушном для юзера {userType}:{userId}")
    public void deleteDeviceFromUser(String userType, String userId, String deviceId) {
        RestAssured.given().filter(new AllureLoggerFilter())
                .baseUri(config.getPushnoyApiURI().toString())
                .header("Accept", "application/json")
                .contentType("application/json")
                .pathParam("app","auto")
                .pathParam("userId", format("%s:%s", userType, userId))
                .pathParam("deviceId", deviceId)
                .when()
                .delete("/{app}/user/{userId}/device/{deviceId}")
                .then().statusCode(200);

    }


    @Step("Получаем сессию авторизованного юзера")
    public String getAuthSession(Account account, String deviceUid, String sessionId) {
        VertisPassportLoginParameters params = new VertisPassportLoginParameters()
                .login(account.getLogin()).password(account.getPassword()).options(new VertisPassportLoginOptions().allowClientLogin(true));

        String tamper = this.getTamperForRequest(params, deviceUid);

        return api.auth().login().body(params)
                .xAuthorizationHeader("Vertis ios-62ca2575df9c74b3958d118afcbb7602")
                .xDeviceUidHeader(deviceUid)
                .xTimestampHeader(tamper)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON())).getSession().getId();
    }

    @Step("Дергаем ручку /hello для привязки девайса в пушном")
    public void sayHelloToAttachDevice(String authSession, String deviceUid){
        api.device().hello().body(new AutoApiHelloRequest().device(new AutoApiDevice()))
                .reqSpec(defaultSpec()).xSessionIdHeader(authSession)
                .xDeviceUidHeader(deviceUid)
                .xAuthorizationHeader("Vertis android-5442cd88c413ada3ce3d36a3d8061fb7")
                .execute(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));
    }
}
