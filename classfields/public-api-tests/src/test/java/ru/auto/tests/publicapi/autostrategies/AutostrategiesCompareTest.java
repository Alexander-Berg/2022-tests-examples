package ru.auto.tests.publicapi.autostrategies;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiAddAutostrategiesRequest;
import ru.auto.tests.publicapi.model.AutoApiBillingAutostrategyAlwaysAtFirstPage;
import ru.auto.tests.publicapi.model.AutoApiBillingAutostrategy;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;
import ru.auto.tests.publicapi.utils.UtilsPublicApi;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.assertj.core.util.Lists.newArrayList;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("PUT /autostrategies")
@GuiceModules(PublicApiDealerModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AutostrategiesCompareTest {
    private static String offerId;
    private static final int WEEK_FORWARD = 7;
    private static final int DAYS_FORWARD = 3;
    private static final int APPLICATIONS_COUNT = 5;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private Account account;

    @Inject
    private PublicApiDealerAdaptor adaptor;

    @Parameter("Категория")
    @Parameterized.Parameter(0)
    public AutoApiOffer.CategoryEnum category;

    @Parameter("Оффер")
    @Parameterized.Parameter(1)
    public String path;

    @Parameter("Параметры автостратегии")
    @Parameterized.Parameter(2)
    public AutoApiAddAutostrategiesRequest request;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {CARS, "offers/dealer_new_cars.json", new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy()
                        .offerId(offerId).fromDate(UtilsPublicApi.getCurrentTime()).toDate(UtilsPublicApi.getTimeDaysForward(WEEK_FORWARD))
                        .maxApplicationsPerDay(APPLICATIONS_COUNT)
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(true).forMarkModelListing(false))))},
                {MOTO, "offers/dealer_moto.json", new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy()
                        .offerId(offerId).fromDate(UtilsPublicApi.getCurrentTime()).toDate(UtilsPublicApi.getTimeDaysForward(WEEK_FORWARD))
                        .maxApplicationsPerDay(APPLICATIONS_COUNT)
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(true).forMarkModelListing(false))))},
                {TRUCKS, "offers/dealer_trucks.json", new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy()
                        .offerId(offerId).fromDate(UtilsPublicApi.getCurrentTime()).toDate(UtilsPublicApi.getTimeDaysForward(WEEK_FORWARD))
                        .maxApplicationsPerDay(APPLICATIONS_COUNT)
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(true).forMarkModelListing(false))))},
                {CARS, "offers/dealer_new_cars.json", new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy()
                        .offerId(offerId).fromDate(UtilsPublicApi.getCurrentTime()).toDate(UtilsPublicApi.getTimeDaysForward(WEEK_FORWARD))
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(true).forMarkModelListing(false))))},
                {CARS, "offers/dealer_new_cars.json", new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy()
                        .offerId(offerId).fromDate(UtilsPublicApi.getCurrentTime()).toDate(UtilsPublicApi.getTimeDaysForward(WEEK_FORWARD))
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(false).forMarkModelListing(true))))},
                {CARS, "offers/dealer_new_cars.json", new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy()
                        .offerId(offerId).fromDate(UtilsPublicApi.getTimeDaysForward(DAYS_FORWARD)).toDate(UtilsPublicApi.getTimeDaysForward(WEEK_FORWARD))
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(true).forMarkModelListing(false))))}
        });
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldOfferAfterPutAutostrategiesHasNoDiffWithProduction() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, category, path).getOfferId();
        request.getAutostrategies().get(0).setOfferId(offerId);

        api.autostrategies().putAutostrategies().body(request)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.userOffers().getMyOffer().categoryPath(category)
                .offerIDPath(offerId).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
