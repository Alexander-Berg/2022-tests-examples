package ru.auto.tests.publicapi.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Description;
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
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplateData;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomLicensePlate;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomVin;

@DisplayName("POST /user/draft/{category}/{offerId}/publish")
@RunWith(Parameterized.class)
@GuiceModules(PublicApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PublishCarsAndTrucksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private AccountManager am;

    @Inject
    private PublicApiAdaptor adaptor;

    @Parameter
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @Parameterized.Parameters(name = "category={0}")
    public static Object[] getParameters() {
        return new AutoApiOffer.CategoryEnum[]{
                CARS,
                TRUCKS
        };
    }

    @Test
    @Description("Публикуем черновик дважды. Сравниваем с продакшеном: должны появиться SAME_SALE")
    public void shouldSameSaleHasNoDiffWithProduction() {
        Account account = am.create();
        String sessionId = adaptor.login(account).getSession().getId();
        String vin = getRandomVin();
        String licensePlate = getRandomLicensePlate();

        adaptor.createOffer(account.getLogin(), sessionId, category);
        OfferTemplateData data = new OfferTemplateData().withPhone(account.getLogin()).withLicensePlate(licensePlate).withVin(vin);

        String offerId = adaptor.createDraft(data, sessionId, category).getOfferId();
        JsonObject response = api.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        String offerIdProd = adaptor.createDraft(data, sessionId, category).getOfferId();
        JsonObject responseProd = prodApi.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerIdProd)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(response, jsonEquals(responseProd).whenIgnoringPaths("offer_id", "offer.service_prices", "offer.url", "offer.mobile_url", "offer.price_history[*].create_timestamp", "offer.price_info.create_timestamp",
                "offer.id", "offer.additional_info.expire_date", "offer.additional_info.update_date", "offer.additional_info.actualize_date", "offer.additional_info.creation_date", "offer.created", "offer.services[*].create_date", "offer.state.panorama_autoru"));
    }
}
