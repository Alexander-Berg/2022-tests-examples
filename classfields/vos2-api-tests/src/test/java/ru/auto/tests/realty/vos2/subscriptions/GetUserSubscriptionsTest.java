package ru.auto.tests.realty.vos2.subscriptions;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.anno.Prod;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithUserIsNotFound;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("GET /api/realty/user/subscriptions/get/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class GetUserSubscriptionsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient vos2;

    @Inject
    @Prod
    private ApiClient prodVos2;

    @Inject
    @Vos
    private Account account;

    @Test
    public void shouldUserSubsciptionsHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.userSubscriptions().getRoute()
                .userIDPath(account.getId()).execute(validatedWith(shouldBeCode(SC_OK)))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomUserId = getRandomLogin();
        vos2.userSubscriptions().getRoute().userIDPath(randomUserId)
                .execute(validatedWith(shouldBe404WithUserIsNotFound(randomUserId)));
    }
}
