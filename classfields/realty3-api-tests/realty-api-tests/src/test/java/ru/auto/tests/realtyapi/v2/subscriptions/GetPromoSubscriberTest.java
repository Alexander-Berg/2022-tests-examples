package ru.auto.tests.realtyapi.v2.subscriptions;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyMailingsPromoStatus;
import ru.auto.tests.realtyapi.v2.model.RealtyMailingsUpdatePromoStatusRequest;
import ru.yandex.qatools.allure.annotations.Title;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getDefaultTestName;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getUid;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Title("PUT /promo/user/{user}")
@GuiceModules(RealtyApiModule.class)
@RunWith(GuiceTestRunner.class)
public class GetPromoSubscriberTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WithNoAuth() {
        api.subscriptionsBase().getSubscriptionStatus()
                .userPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404InvalidUserRef() {
        api.subscriptionsBase().getSubscriptionStatus()
                .reqSpec(authSpec())
                .userPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldSeeEqualResponses() {
        Account account = am.create();
        RealtyMailingsUpdatePromoStatusRequest request = random(RealtyMailingsUpdatePromoStatusRequest.class)
                .email(account.getLogin());

        RealtyMailingsPromoStatus creationResponse = api.subscriptionsBase().updateSubscriptionStatus()
                .reqSpec(authSpec())
                .body(request)
                .userPath(getUid(account))
                .executeAs(validatedWith(shouldBe200Ok())).getResponse()
                .firstName(getDefaultTestName());

        RealtyMailingsPromoStatus getResponse = api.subscriptionsBase().getSubscriptionStatus()
                .reqSpec(authSpec())
                .userPath(getUid(account))
                .executeAs(validatedWith(shouldBe200Ok())).getResponse();

        org.assertj.core.api.Assertions.assertThat(creationResponse.getCreatedTimestamp())
                .describedAs("Responses should has almost equal timestamps")
                .isBeforeOrEqualTo(getResponse.getCreatedTimestamp());

        Assertions.assertThat(creationResponse.createdTimestamp(null))
                .describedAs("Responses must be the same")
                .isEqualTo(getResponse.createdTimestamp(null));

    }
}
