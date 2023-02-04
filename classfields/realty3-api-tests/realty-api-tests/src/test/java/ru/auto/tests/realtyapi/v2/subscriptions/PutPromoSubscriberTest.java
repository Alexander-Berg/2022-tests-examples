package ru.auto.tests.realtyapi.v2.subscriptions;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiError;
import ru.auto.tests.realtyapi.v2.model.RealtyMailingsPromoStatus;
import ru.auto.tests.realtyapi.v2.model.RealtyMailingsUpdatePromoStatusRequest;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("PUT /promo/user/{user}")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class PutPromoSubscriberTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WithNoAuth() {
        api.subscriptionsBase().updateSubscriptionStatus()
                .userPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404InvalidUserRef() {
        api.subscriptionsBase().updateSubscriptionStatus()
                .reqSpec(authSpec())
                .userPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldSee400WithNoEmail() {
        Account account = am.create();
        RealtyMailingsUpdatePromoStatusRequest request = random(RealtyMailingsUpdatePromoStatusRequest.class)
                .email(null);

        RealtyApiError error = api.subscriptionsBase().updateSubscriptionStatus()
                .reqSpec(authSpec())
                .body(request)
                .userPath(getUid(account))
                .executeAs(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR))).getError();

        Assertions.assertThat(error.getMessage()).contains("Bad Request");
    }

    @Test
    public void shouldSee200WithValidEntity() {
        Account account = am.create();
        RealtyMailingsUpdatePromoStatusRequest request = random(RealtyMailingsUpdatePromoStatusRequest.class)
                .email(account.getLogin());
        RealtyMailingsPromoStatus response = api.subscriptionsBase().updateSubscriptionStatus()
                .reqSpec(authSpec())
                .body(request)
                .userPath(getUid(account))
                .executeAs(validatedWith(shouldBe200Ok())).getResponse();

        Assertions.assertThat(request.getSubscriptionId())
                .describedAs("SubscriptionId should be the same")
                .isEqualTo(response.getSubscriptionId());
        Assertions.assertThat(request.getGeoId())
                .describedAs("geoId should be the same")
                .isEqualTo(response.getGeoId());
        Assertions.assertThat(request.getEmail())
                .describedAs("email should be the same")
                .isEqualTo(response.getEmail());
        Assertions.assertThat(request.getIsAgreed())
                .describedAs("Agreement status should be the same")
                .isEqualTo(response.getIsAgreed());
    }
}
