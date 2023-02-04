package ru.auto.tests.publicapi.autostrategies;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.model.AutoApiAutostrategyIdsList;
import ru.auto.tests.publicapi.model.AutoApiBillingAutostrategy;
import ru.auto.tests.publicapi.model.AutoApiBillingAutostrategyId;
import ru.auto.tests.publicapi.module.PublicApiDealerModule;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.assertj.core.util.Lists.newArrayList;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiBillingAutostrategyId.AutostrategyTypeEnum.ALWAYS_AT_FIRST_PAGE;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("PUT /autostrategies/delete")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiDealerModule.class)
public class AutostrategiesDeleteTest {
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
        api.autostrategies().deleteAutostrategies().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee401WithoutSession() {
        api.autostrategies().deleteAutostrategies().body(new AutoApiAutostrategyIdsList())
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithoutOfferId() {
        String sessionId = adaptor.login(account).getSession().getId();

        api.autostrategies().deleteAutostrategies().body(new AutoApiAutostrategyIdsList().ids(newArrayList(new AutoApiBillingAutostrategyId().autostrategyType(ALWAYS_AT_FIRST_PAGE))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSee400WithInvalidOfferId() {
        String sessionId = adaptor.login(account).getSession().getId();

        api.autostrategies().deleteAutostrategies()
                .body(new AutoApiAutostrategyIdsList().ids(newArrayList(new AutoApiBillingAutostrategyId().offerId(Utils.getRandomString())
                        .autostrategyType(ALWAYS_AT_FIRST_PAGE))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSuccessfullyDeleteAutostrategies() {
        String sessionId = adaptor.login(account).getSession().getId();
        String offerId = adaptor.createDealerOffer(sessionId, CARS, PATH).getOfferId();
        adaptor.createAutostrategiesForOffer(sessionId, offerId);

        api.autostrategies().deleteAutostrategies()
                .body(new AutoApiAutostrategyIdsList().ids(newArrayList(new AutoApiBillingAutostrategyId().autostrategyType(ALWAYS_AT_FIRST_PAGE).offerId(offerId))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess()));

        List<AutoApiBillingAutostrategy> response = api.userOffers().getMyOffer().categoryPath(CARS).offerIDPath(offerId).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getOffer().getAutostrategies();

        Assertions.assertThat(response).isNull();
    }
}
