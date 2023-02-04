package ru.auto.tests.realtyapi.v1.blog;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_FIELDS;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_VALUES;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.enums.YesOrNo.YES;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;

@Title("GET /blog/posts")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetBlogPostsTest {

    private static final int NEGATIVE_SIZE = -1;
    private static final int ONE_POST = 1;

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

    @Test
    public void shouldSee403WithoutHeaders() {
        api.blog().getBlogPostsRoute().sizeQuery(getRandomShortInt())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldShouldSeeNullWithNegativeSize() {
        JsonObject response = api.blog().getBlogPostsRoute().reqSpec(authSpec())
                .sizeQuery(NEGATIVE_SIZE)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.getAsJsonArray("posts"))
                .describedAs("Ответ не содержит постов")
                .isNull();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldShouldSeeOnePostWithNegativeSizeAndParameters() {
        JsonObject response = api.blog().getBlogPostsRoute().reqSpec(authSpec())
                .sizeQuery(NEGATIVE_SIZE)
                .typeQuery(SELL)
                .categoryQuery(APARTMENT)
                .newFlatQuery(YES)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.getAsJsonArray("posts"))
                .describedAs("Ответ содержит один пост")
                .hasSize(ONE_POST);
    }

    @Test
    public void shouldRealtyPostsHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.blog().getBlogPostsRoute()
                .reqSpec(authSpec()).authorizationHeader(token)
                .sizeQuery(1)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi))
                .whenIgnoringPaths("response.posts[*].tags")
                .when(IGNORING_VALUES, IGNORING_EXTRA_FIELDS, IGNORING_EXTRA_ARRAY_ITEMS));
    }
}
