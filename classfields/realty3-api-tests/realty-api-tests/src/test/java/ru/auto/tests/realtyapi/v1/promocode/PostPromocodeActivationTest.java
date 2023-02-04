package ru.auto.tests.realtyapi.v1.promocode;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.restassured.builder.ResponseSpecBuilder;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.bean.Promocode;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_PAYMENT_REQUIRED;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.apiAwait;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("POST /user/{uid}/promocode/{promocodeId}/activate")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PostPromocodeActivationTest {

    private static final String USED_PROMOCODE = "ERROR_ADD_USED";
    private static final String OUTDATED_PROMOCODE = "ERROR_ADD_EXPIRED";
    private static final String ALREADY_ACTIVATED_PROMOCODE = "ERROR_ADD_USED_YOURS";
    private static final String USER_IS_BLACKLISTED = "ERROR_ADD_BLACKLIST";
    private static final int VALID_COUNT = 1000;
    private static final int VALID_ACTIVATIONS = 10;

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

    @Parameter
    @Parameterized.Parameter(0)
    public String tag;

    @Parameterized.Parameters(name = "tag={0}")
    public static Object[] getParameters() {
        return new Object[]{"money", "raising", "premium"};
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserHasActivatedPromocode() {
        Account account = am.create();
        String uid = account.getId();

        Promocode promocode = adaptor.createPromocode(tag, VALID_COUNT, VALID_ACTIVATIONS, VALID_ACTIVATIONS);
        String promocodeId = promocode.getCode();

        api.promocode().activatePromocodeRoute().reqSpec(authSpec())
                .uidPath(uid)
                .promocodeIdPath(promocodeId)
                .execute(validatedWith(shouldBe200Ok()));

        apiAwait().atMost(60, SECONDS)
                .until(() -> adaptor.getUsersPromocode(uid), iterableWithSize(1));

        JsonArray userPromocodes = adaptor.getUsersPromocode(uid);
        Assertions.assertThat(userPromocodes.get(0).getAsJsonObject().getAsJsonObject("origin").get("id").getAsString())
                .describedAs("Пользователь должен иметь зарегистрированный промокод")
                .contains(promocodeId);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserCanActivatePromocodeWithAlias() {
        Account account = am.create();
        String uid = account.getId();

        Promocode promocode = adaptor.createPromocode(tag, VALID_COUNT, VALID_ACTIVATIONS, VALID_ACTIVATIONS);
        String promocodeAlias = promocode.getOneAlias();

        api.promocode().activatePromocodeRoute().reqSpec(authSpec())
                .uidPath(uid)
                .promocodeIdPath(promocodeAlias)
                .execute(validatedWith(shouldBe200Ok()));

        apiAwait().atMost(60, SECONDS)
                .until(() ->  adaptor.getUsersPromocode(uid), iterableWithSize(1));

        JsonArray userPromocodes = adaptor.getUsersPromocode(uid);
        Assertions.assertThat(userPromocodes.get(0).getAsJsonObject().getAsJsonObject("origin").get("id").getAsString())
                .describedAs("Пользователь должен иметь зарегистрированный промокод")
                .contains(promocode.getCode());
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldPromocodeCanBeActivatedOnce() {
        int activations = 1;
        Account firstUser = am.create();
        String firstUserUid = firstUser.getId();

        Account secondUser = am.create();
        String secondUserUid = secondUser.getId();

        Promocode promocode = adaptor.createPromocode(tag, VALID_COUNT, activations, activations);
        String promocodeId = promocode.getCode();

        api.promocode().activatePromocodeRoute().reqSpec(authSpec())
                .uidPath(firstUserUid)
                .promocodeIdPath(promocodeId)
                .execute(validatedWith(shouldBe200Ok()));

        JsonObject response = api.promocode().activatePromocodeRoute().reqSpec(authSpec())
                .uidPath(secondUserUid)
                .promocodeIdPath(promocodeId)
                .execute(validatedWith(shouldBeCode(SC_PAYMENT_REQUIRED)))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("error")
                .getAsJsonObject("data");

        Assertions.assertThat(response.get("code").getAsString())
                .describedAs("Нельзя зарегистрировать промокод больше указанного числа раз = " + activations)
                .isEqualTo(USED_PROMOCODE);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserCanNotUseOutdatedPromocode() {
        Account account = am.create();
        String uid = account.getId();

        Promocode promocode = adaptor.createOutdatedPromocode(tag, VALID_COUNT, VALID_ACTIVATIONS);
        String promocodeId = promocode.getCode();

        JsonObject response = api.promocode().activatePromocodeRoute().reqSpec(authSpec())
                .uidPath(uid)
                .promocodeIdPath(promocodeId)
                .execute(validatedWith(shouldBeCode(SC_PAYMENT_REQUIRED)))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("error")
                .getAsJsonObject("data");

        Assertions.assertThat(response.get("code").getAsString())
                .describedAs("Нельзя зарегистрировать устаревший промокод")
                .isEqualTo(OUTDATED_PROMOCODE);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserCanNotActivateOnePromocodeTwice() {
        Account account = am.create();
        String uid = account.getId();

        Promocode promocode = adaptor.createPromocode(tag, VALID_COUNT, VALID_ACTIVATIONS, 1);
        String promocodeId = promocode.getCode();

        Function<ResponseSpecBuilder, JsonObject> activate = responseSpec -> api.promocode().activatePromocodeRoute()
                .reqSpec(authSpec())
                .uidPath(uid)
                .promocodeIdPath(promocodeId)
                .execute(validatedWith(responseSpec))
                .as(JsonObject.class, GSON);

        activate.apply(shouldBe200Ok());
        JsonObject response = activate.apply(shouldBeCode(SC_PAYMENT_REQUIRED))
                .getAsJsonObject("error")
                .getAsJsonObject("data");

        Assertions.assertThat(response.get("code").getAsString())
                .describedAs("Нельзя зарегистрировать промокод два раза")
                .isEqualTo(ALREADY_ACTIVATED_PROMOCODE);
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUserCanActivateOnePromocodeTwice() {
        Account account = am.create();
        String uid = account.getId();

        Promocode promocode = adaptor.createPromocode(tag, VALID_COUNT, VALID_ACTIVATIONS, 2);
        String promocodeId = promocode.getCode();

        Consumer<ResponseSpecBuilder> activate = responseSpec -> api.promocode().activatePromocodeRoute()
                .reqSpec(authSpec())
                .uidPath(uid)
                .promocodeIdPath(promocodeId)
                .execute(validatedWith(responseSpec));

        activate.accept(shouldBe200Ok());
        activate.accept(shouldBe200Ok());
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldBlacklistedUserCanNotUsePromocode() {
        Account account = am.create();
        String uid = account.getId();

        Promocode promocode = adaptor.createPromocodeWithBlacklist(tag, VALID_COUNT, VALID_ACTIVATIONS, uid);
        String promocodeId = promocode.getCode();

        JsonObject response = api.promocode().activatePromocodeRoute()
                .reqSpec(authSpec())
                .uidPath(uid)
                .promocodeIdPath(promocodeId)
                .execute(validatedWith(shouldBeCode(SC_PAYMENT_REQUIRED)))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("error")
                .getAsJsonObject("data");

        Assertions.assertThat(response.get("code").getAsString())
                .describedAs("Пользователь в черном списке не может иметь право активации промокода")
                .isEqualTo(USER_IS_BLACKLISTED);
    }


}
