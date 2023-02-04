package ru.auto.tests.realtyapi.v1.stat;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.restassured.builder.RequestSpecBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getYesterdayDate;
import static ru.auto.tests.realtyapi.v1.api.ArchiveApi.ArchiveSearchRouteOper.CATEGORY_QUERY;
import static ru.auto.tests.realtyapi.v1.api.ArchiveApi.ArchiveSearchRouteOper.TYPE_QUERY;
import static ru.auto.tests.realtyapi.v1.api.StatApi.UserTotalShowsOper.END_TIME_QUERY;
import static ru.auto.tests.realtyapi.v1.api.StatApi.UserTotalShowsOper.SPAN_QUERY;
import static ru.auto.tests.realtyapi.v1.api.StatApi.UserTotalShowsOper.START_TIME_QUERY;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOfferCategory;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOfferType;


@Title("GET /stat/shows/total/user/{uid}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetStatShowsTotalUserCompareTest {

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

    @Parameter("Спецификация")
    @Parameterized.Parameter(0)
    public Consumer<RequestSpecBuilder> reqSpec;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        int span = getRandomShortInt();
        String startTime = getYesterdayDate();
        String endTime = Instant.now().toString();

        List<Consumer<RequestSpecBuilder>> params = newArrayList();
        defaultOfferType().forEach(offerType ->
                params.add(req -> req.addParam(TYPE_QUERY, offerType)
                        .addParam(CATEGORY_QUERY, RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT)
                        .addParam(SPAN_QUERY, span)));
        defaultOfferCategory().forEach(offerCategory ->
                params.add(req -> req.addParam(TYPE_QUERY, RealtyResponseOfferResponse.OfferTypeEnum.SELL)
                        .addParam(CATEGORY_QUERY, offerCategory)
                        .addParam(SPAN_QUERY, span)));

        params.add(req -> req.addParam(TYPE_QUERY, RealtyResponseOfferResponse.OfferTypeEnum.SELL)
                .addParam(CATEGORY_QUERY, RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT)
                .addParam(START_TIME_QUERY, startTime)
                .addParam(END_TIME_QUERY, endTime));
        return params;
    }

    @Test
    public void shouldHasNoDiffWithProductionWith() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.stat().userTotalShows()
                .reqSpec(authSpec()).xAuthorizationHeader(token)
                .uidPath(account.getId())
                .reqSpec(reqSpec)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
