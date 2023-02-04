package ru.auto.tests.publicapi.autostrategies;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.model.AutoApiAddAutostrategiesRequest;
import ru.auto.tests.publicapi.model.AutoApiBillingAutostrategyAlwaysAtFirstPage;
import ru.auto.tests.publicapi.model.AutoApiBillingAutostrategy;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.core.util.Lists.newArrayList;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getCurrentTime;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getTimeDaysForward;

@DisplayName("PUT /autostrategies")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiDealerModule.class)
public class AutostrategiesTest {
    private static final int DAYS_FORWARD = 3;
    private static final int APPLICATIONS_COUNT = 5;
    private static final String PATH = "offers/dealer_new_cars.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private Account account;

    @Inject
    private PublicApiDealerAdaptor adaptor;

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee403WhenNoAuth() {
        api.autostrategies().putAutostrategies().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee401WithoutSession() {
        api.autostrategies().putAutostrategies().body(new AutoApiAddAutostrategiesRequest())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutBody() {
        api.autostrategies().putAutostrategies().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutOfferId() {
        String sessionId = adaptor.login(account).getSession().getId();

        api.autostrategies().putAutostrategies()
                .body(new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy()
                        .fromDate(getCurrentTime()).toDate(getTimeDaysForward(DAYS_FORWARD))
                        .maxApplicationsPerDay(APPLICATIONS_COUNT)
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(true).forMarkModelListing(false)))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutFromDate() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, PATH).getOfferId();

        api.autostrategies().putAutostrategies()
                .body(new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy()
                        .offerId(offerId).toDate(getTimeDaysForward(DAYS_FORWARD)).maxApplicationsPerDay(APPLICATIONS_COUNT)
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(true).forMarkModelListing(false)))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutToDate() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, PATH).getOfferId();

        api.autostrategies().putAutostrategies()
                .body(new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy()
                        .offerId(offerId).fromDate(getCurrentTime()).maxApplicationsPerDay(APPLICATIONS_COUNT)
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(true).forMarkModelListing(false)))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutAlwaysAtFirstPage() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, PATH).getOfferId();

        api.autostrategies().putAutostrategies()
                .body(new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy().fromDate(getCurrentTime())
                        .offerId(offerId).toDate(getTimeDaysForward(DAYS_FORWARD)).maxApplicationsPerDay(APPLICATIONS_COUNT))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithEmptyAlwaysAtFirstPage() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, PATH).getOfferId();

        api.autostrategies().putAutostrategies()
                .body(new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy().fromDate(getCurrentTime())
                        .offerId(offerId).toDate(getTimeDaysForward(DAYS_FORWARD)).maxApplicationsPerDay(APPLICATIONS_COUNT)
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage()))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSeeSuccessWithBothTrueAlwaysAtFirstPage() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, PATH).getOfferId();

        api.autostrategies().putAutostrategies()
                .body(new AutoApiAddAutostrategiesRequest().autostrategies(newArrayList(new AutoApiBillingAutostrategy().fromDate(getCurrentTime())
                        .offerId(offerId).toDate(getTimeDaysForward(DAYS_FORWARD)).maxApplicationsPerDay(APPLICATIONS_COUNT)
                        .alwaysAtFirstPage(new AutoApiBillingAutostrategyAlwaysAtFirstPage().forMarkModelGenerationListing(true).forMarkModelListing(true)))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));
    }
}
