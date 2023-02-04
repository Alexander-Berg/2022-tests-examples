package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.jayway.jsonpath.JsonPath;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.JSON;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplate;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplateData;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.testdata.TestData.defaultOffersByCategories;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomLicensePlate;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomVin;

@DisplayName("POST /user/draft/{category}/{offerId}/publish")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PublishDraftCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @Parameter("Оффер")
    @Parameterized.Parameter(1)
    public String offer;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0} - {1}")
    public static List<Object[]> getParameters() {
        return Arrays.asList(defaultOffersByCategories());
    }

    @Test
    @Description("Создаем оффер для разных черновиков. Сравниваем с продакшеном")
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();
        Account foreignAccount = am.create();
        String vin = getRandomVin();
        String licensePlate = getRandomLicensePlate();
        String offerSource = new OfferTemplate().process(offer, new OfferTemplateData().withPhone(account.getLogin())
                .withLicensePlate(licensePlate).withVin(vin));
        String foreignOfferSource = new OfferTemplate().process(offer, new OfferTemplateData().withPhone(foreignAccount.getLogin())
                .withLicensePlate(licensePlate).withVin(vin));

        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDraft(sessionId, category, offerSource).getOfferId();

        JsonObject response = api.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);
        response = ignoreProlongationForced(response);

        String sessionIdProd = adaptor.login(foreignAccount).getSession().getId();
        String offerIdProd = adaptor.createDraft(sessionIdProd, category, foreignOfferSource).getOfferId();
        JsonObject responseProd = prodApi.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerIdProd)
                .xSessionIdHeader(sessionIdProd).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(response, jsonEquals(responseProd).whenIgnoringPaths("offer_id", "offer.url",
                "offer.mobile_url", "offer.price_history[*].create_timestamp", "offer.price_info.create_timestamp",
                "offer.id", "offer.additional_info.expire_date", "offer.additional_info.update_date",
                "offer.additional_info.actualize_date", "offer.additional_info.creation_date",
                "offer.user_ref", "offer.private_seller.phones[*].phone", "offer.seller.phones[*].phone",
                "offer.seller.phones[*].original", "offer.private_seller.phones[*].original", "offer.created",
                "offer.services[*].create_date", "offer.service_prices[*].prolongation_forced", "offer.state.panorama_autoru",
                "offer.additional_info.other_offers_show_info"));
    }

    /*
     * caused by https://st.yandex-team.ru/AUTORUAPI-5382
     * hack response to solve problem: https://github.com/lukas-krecan/JsonUnit/issues/128
     */
    @Step("Получаем ответ с проставленным флагом «prolongation_forced» (будет игнорирован при сравнении)")
    private JsonObject ignoreProlongationForced(JsonObject response) {
        LinkedHashMap hackedResponse = JsonPath.parse(response.toString())
                .put("offer.service_prices[?(@.service == 'package_turbo')]", "prolongation_forced", true).read("$");
        return new JSON().deserialize(new Gson().toJson(hackedResponse, Map.class), JsonObject.class);
    }
}
