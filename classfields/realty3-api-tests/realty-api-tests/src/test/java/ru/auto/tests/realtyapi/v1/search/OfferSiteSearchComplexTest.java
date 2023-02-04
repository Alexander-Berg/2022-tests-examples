package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;

@Title("GET /search/offerWithSiteSearch.json")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class OfferSiteSearchComplexTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    @Issue("VERTISTEST-1147")
    @Owner(ARTEAMO)
    public void shouldSeeOfferWithReassignmentWithinTwoMinutes() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token,
                "offers/apartment_complex_reassignment_sell.ftl")
                .getResponse().getId();

        adaptor.waitOfferIsInSearcher(offerId);
    }

    @Test
    @Issue("VERTISTEST-1147")
    @Owner(ARTEAMO)
    public void shouldSeeOfferWithinTwoMinutes() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token,
                "offers/apartment_complex_sell.ftl")
                .getResponse().getId();

        adaptor.waitOfferIsInSearcher(offerId);
    }
}
