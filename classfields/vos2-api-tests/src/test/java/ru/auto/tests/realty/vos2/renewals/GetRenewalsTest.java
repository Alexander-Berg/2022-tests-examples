package ru.auto.tests.realty.vos2.renewals;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.Prod;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNoEnumConstant;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getObjectFromJson;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultRenewalsShowStatus;

@DisplayName("GET /api/realty/renewals/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class GetRenewalsTest {
//
//    private static final String RENEWALS_UPDATE_REQUEST_PATH = "testdata/renewals_update_request_body.json";
//
//    @Rule
//    @Inject
//    public RuleChain defaultRules;
//
//    @Inject
//    private ApiClient vos2;
//
//    @Inject
//    @Prod
//    private ApiClient prodVos2;
//
//    @Inject
//    @Vos
//    private Account account;
//
//    @Inject
//    private Vos2ApiAdaptor adaptor;
//
//    private String id;
//
//
//    @DataProvider
//    public static Object[] showStatuses() {
//        return defaultRenewalsShowStatus();
//    }
//
//    @Test
//    @UseDataProvider("showStatuses")
//    public void shouldSuccessGetUserRenewals(String showStatus) {
//        id = adaptor.createOffer(account.getId()).getId().get(0);
//
//        vos2.renewals().updatingRoute().userIDPath(account.getId()).reqSpec(jsonBody(getBodyRequest()))
//                .execute(validatedWith(shouldBeCode(SC_OK)));
//
//        Function<ApiClient, JsonObject> request = apiClient -> apiClient.renewals().getRoute()
//                .userIDPath(account.getId()).showStatusQuery(showStatus)
//                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class, GSON);
//
//        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
//    }
//
//    @Test
//    public void shouldSee400WithoutStatus() {
//        vos2.renewals().getRoute().userIDPath(account.getId()).showStatusQuery(StringUtils.EMPTY)
//                .execute(validatedWith(shouldBe400WithNoEnumConstant()));
//    }
//
//    private JsonObject getBodyRequest() {
//        JsonObject reqBody = getObjectFromJson(JsonObject.class, RENEWALS_UPDATE_REQUEST_PATH);
//        reqBody.getAsJsonObject("renewals").remove("offerId");
//        reqBody.getAsJsonObject("renewals").add(id, reqBody.getAsJsonObject("renewals")
//                .getAsJsonArray("offerId"));
//        return reqBody;
//    }
}
