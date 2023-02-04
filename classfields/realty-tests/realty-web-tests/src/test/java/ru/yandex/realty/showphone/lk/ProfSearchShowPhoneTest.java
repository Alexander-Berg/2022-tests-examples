package ru.yandex.realty.showphone.lk;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.anno.ProfsearchAccount;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.mock.OfferPhonesResponse;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.PassportSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.SEARCH;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.OfferPhonesResponse.offersPhonesTemplate;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_DASHES;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@DisplayName("Показ телефона. Профпоиск")
@Feature(LISTING)
@Link("https://st.yandex-team.ru/VERTISTEST-1599")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class ProfSearchShowPhoneTest {

    private static final String TEST_PHONE = "+71112223344";
    private static final String SECOND_TEST_PHONE = "+72225556677";
    private static final String PHONE_OPENED = "_phone_opened";

    private MockOffer offer;
    private OfferPhonesResponse offersPhonesTemplate;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @ProfsearchAccount
    @Inject
    private Account account;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT);
        passportSteps.login(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в листинге")
    public void shouldSeePhoneNormalCaseProfSearch() {
        offersPhonesTemplate = offersPhonesTemplate().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();
        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).open();

        basePageSteps.onProfSearchPage().offer(FIRST).showPhoneButton().click();
        basePageSteps.onProfSearchPage().offer(FIRST).showPhoneButton()
                .waitUntil(hasClass(containsString(PHONE_OPENED)));
        basePageSteps.onProfSearchPage().offer(FIRST).showPhoneButton()
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_BRACKETS)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("При 500-ке телефон показывается тот что был в верстке")
    public void shouldSeePhone500ProfSearch() {
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .offerPhonesStub500(offer.getOfferId())
                .createWithDefaults();

        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).open();
        String phone = basePageSteps.onProfSearchPage().offer(FIRST).showPhoneButton().getText();
        basePageSteps.onProfSearchPage().offer(FIRST).showPhoneButton().click();
        basePageSteps.onProfSearchPage().offer(FIRST).showPhoneButton().should(hasText(phone));
    }

    @Test
    @Ignore("КАК СДЕЛАТЬ ТАК ЧТОБЫ ПОКАЗЫВАЛОСЬ ДВА НОМЕРА?")
    @Owner(KANTEMIROV)
    @DisplayName("Показ двух телефонов")
    public void shouldSeeTwoPhoneProfSearch() {
        offersPhonesTemplate = offersPhonesTemplate().addPhone(TEST_PHONE).addPhone(SECOND_TEST_PHONE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().offers(asList(offer)).build())
                .offerPhonesStub(offer.getOfferId(), offersPhonesTemplate.build())
                .createWithDefaults();

        urlSteps.testing().path(MANAGEMENT_NEW).path(SEARCH).open();
        basePageSteps.onProfSearchPage().offer(FIRST).showPhoneButton().click();
        String phones = format("%s, %s", makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_DASHES),
                makePhoneFormatted(SECOND_TEST_PHONE, PHONE_PATTERN_DASHES));
        basePageSteps.onProfSearchPage().offer(FIRST).showPhoneButton().should(hasText(phones));
    }
}