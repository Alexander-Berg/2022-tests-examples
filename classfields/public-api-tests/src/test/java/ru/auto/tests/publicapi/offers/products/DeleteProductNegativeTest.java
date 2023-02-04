package ru.auto.tests.publicapi.offers.products;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400IncorrectOfferIdError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe400UnknownCategoryError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401NeedAuthentication;

/**
 * Created by dskuznetsov on 26.12.18
 */

@DisplayName("DELETE /user/offers/{category}/{offerID}/products")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiDealerModule.class)
public class DeleteProductNegativeTest {
    private static final String DEFAULT_PRODUCT = "all_sale_special";
    private static final String DEFAULT_OFFER_PATH = "offers/dealer_new_cars.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiDealerAdaptor adaptor;

    @Inject
    private Account account;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.userOffers().deleteProducts().categoryPath(Utils.getRandomString()).offerIDPath(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee401WithoutSessionId() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, DEFAULT_OFFER_PATH).getOfferId();

        api.userOffers().deleteProducts().categoryPath(CARS).offerIDPath(offerId).productQuery(DEFAULT_PRODUCT)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe401NeedAuthentication()));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithUnknownCategory() {
        String unknownCategory = Utils.getRandomString();
        api.userOffers().deleteProducts().categoryPath(unknownCategory).offerIDPath(Utils.getRandomString())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400UnknownCategoryError(unknownCategory)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithIncorrectOfferId() {
        String incorrectOfferId = Utils.getRandomString();
        api.userOffers().deleteProducts().categoryPath(CARS).offerIDPath(incorrectOfferId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBe400IncorrectOfferIdError(incorrectOfferId)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutBody() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, DEFAULT_OFFER_PATH).getOfferId();

        api.userOffers().deleteProducts().categoryPath(CARS).offerIDPath(offerId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee404WhenNoPaidService() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, DEFAULT_OFFER_PATH).getOfferId();

        AutoApiErrorResponse response = api.userOffers().deleteProducts().categoryPath(CARS).offerIDPath(offerId)
                .productQuery(DEFAULT_PRODUCT)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN))).as(AutoApiErrorResponse.class);

        AutoruApiModelsAssertions.assertThat(response).hasDetailedError("Action deactivate forbidden");
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithInvalidProduct() {
        String invalidProductCode = Utils.getRandomString();
        String detailedError = String.format("Invalid autoru good product: [%s]. Available values: [all_sale_premium, all_sale_special, all_sale_fresh, all_sale_badge, package_turbo]", invalidProductCode);
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, DEFAULT_OFFER_PATH).getOfferId();

        AutoApiErrorResponse response = api.userOffers().deleteProducts().categoryPath(CARS).offerIDPath(offerId)
                .productQuery(invalidProductCode)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(AutoApiErrorResponse.class);
        AutoruApiModelsAssertions.assertThat(response).hasDetailedError(detailedError);
    }
}
