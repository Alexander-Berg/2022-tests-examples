package ru.auto.tests.realtyapi.v1.subscriptions;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.SubscriptionResponse;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse;
import ru.auto.tests.realtyapi.v1.model.SubscriptionRequest;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.assertj.Assertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getValidSubscriptionRequest;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOfferCategory;



@Title("PUT /user/subscriptions/{id}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Log4j
@Ignore("HTTP status 409")
public class UpdateUserSubscriptionSuccessTest {
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
    public RealtyResponseOfferResponse.OfferCategoryEnum offerCategory;

    @Parameterized.Parameters(name = "offerCategory={0}")
    public static List<RealtyResponseOfferResponse.OfferCategoryEnum> getParameters() {
        return defaultOfferCategory();
    }

    @Test
    public void shouldSuccessUpdateSubscription() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        SubscriptionRequest reqBody = getValidSubscriptionRequest();

        String subscriptionId = adaptor.createSubscriptionWithRandomBody().getResponse().getId();
        SubscriptionResponse beforeUpdate = adaptor.getSubscriptionById(subscriptionId);

        SubscriptionResponse updateResp = api.subscriptions().updateSubscriptionRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .idPath(subscriptionId)
                .body(reqBody)
                .execute(validatedWith(shouldBe200OkJSON())).as(SubscriptionResponse.class, GSON);

        assertThat(updateResp.getResponse().getDelivery().getPush()).hasPeriod((long) reqBody.getPeriod())
                .hasToken(reqBody.getToken());
        assertThat(updateResp.getResponse().getView()).hasTitle(reqBody.getTitle());
        assertThat(updateResp.getResponse().getRequest())
                .hasHttpQuery(beforeUpdate.getResponse().getRequest().getHttpQuery());
    }
}
