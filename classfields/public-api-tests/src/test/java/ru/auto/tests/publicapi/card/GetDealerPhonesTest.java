package ru.auto.tests.publicapi.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import net.javacrumbs.jsonunit.core.internal.Options;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;

import java.util.function.Function;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_VALUES;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("GET /offer/{category}/{offerID}/phones")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiDealerModule.class)
public class GetDealerPhonesTest {
    private static final String PATH = "offers/dealer_new_cars.json";

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

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldDealerOfferPhonesHasNoDiffWithProduction() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, PATH).getOfferId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.offerCard().getOfferPhones()
                .categoryPath(CARS).offerIDPath(offerId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).withOptions(Options.empty()
                .with(IGNORING_VALUES)));
    }
}
