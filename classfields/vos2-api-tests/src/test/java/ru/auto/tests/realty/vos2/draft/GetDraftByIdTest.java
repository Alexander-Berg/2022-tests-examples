package ru.auto.tests.realty.vos2.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
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
import static java.lang.String.format;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNumberFormatException;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithOfferNotFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultOffers;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("GET /api/realty/draft/{userID}/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class GetDraftByIdTest {

    @Inject
    private ApiClient vos2;

    @Inject
    @Prod
    private ApiClient prodVos2;

    @Inject
    private Vos2ApiAdaptor adaptor;

    @Inject
    @Vos
    private Account account;

    private String id;

    @DataProvider
    public static Object[] drafts() {
        return defaultOffers();
    }

    @Test
    @UseDataProvider("drafts")
    public void shouldDraftHasNotDiffWithProduction(String draft) {
        id = adaptor.createDraft(getUserIdForDraft(account.getId()), getResourceAsString(draft)).getId().get(0);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().getDraftRoute()
                .userIDPath(getUserIdForDraft(account.getId())).offerIDPath(id)
                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        String randomVosId = getRandomString();
        id = getRandomString();
        vos2.draft().getDraftRoute().userIDPath(getUserIdForDraft(randomVosId)).offerIDPath(id)
                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldSee400ForInvalidDraft() {
        String invalidDraftId = getRandomString();
        vos2.draft().getDraftRoute().userIDPath(getUserIdForDraft(account.getId())).offerIDPath(invalidDraftId)
                .execute(validatedWith(shouldBe400WithNumberFormatException(invalidDraftId)));
    }

    @Test
    public void shouldSee400ForNotExistDraft() {
        String randomDraftId = getRandomLogin();
        vos2.draft().getDraftRoute().userIDPath(getUserIdForDraft(account.getId())).offerIDPath(randomDraftId)
                .execute(validatedWith(shouldBe404WithOfferNotFound(account.getId(), randomDraftId)));
    }

    private String getUserIdForDraft(String userId) {
        return format("login:%s", userId);
    }
}
