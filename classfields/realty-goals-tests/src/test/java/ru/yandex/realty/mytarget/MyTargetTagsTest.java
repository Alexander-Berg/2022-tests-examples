package ru.yandex.realty.mytarget;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.MockOffer.RENT_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.RENT_COMMERCIAL;
import static ru.yandex.realty.mock.MockOffer.RENT_HOUSE;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.SELL_COMMERCIAL_WAREHOUSE;
import static ru.yandex.realty.mock.MockOffer.SELL_HOUSE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.step.ProxySteps.AD_MAIL_RU;

@DisplayName("Mytarget для Купить квартиру, дом коммерческую, снять ")
@Feature(GOALS)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class MyTargetTagsTest {

    private static final String SHOW_PHONE_PARAM = "purchase";
    private static final String ADD_TO_FEV_PARAM = "cart";
    private static final String OPEN_PAGE_PARAM = "product";

    private String offerId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private ProxySteps proxy;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String type;

    @Parameterized.Parameter(2)
    public String listParam;

    @Parameterized.Parameters(name = "{0}. Клик на «Показать телефон»")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Купить квартиру", SELL_APARTMENT, "2"},
                {"Купить дом", SELL_HOUSE, "5"},
                {"Купить коммерческую - склад", SELL_COMMERCIAL_WAREHOUSE, "7"},
                {"Снять квартиру", RENT_APARTMENT, "1"},
                {"Снять дом", RENT_HOUSE, "4"},
                {"Снять коммерческую - участок", RENT_COMMERCIAL, "6"},
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(mockOffer(type))).build()).createWithDefaults();
        proxy.clearHar();
        offerId = mockOffer(type).getOfferId();
        urlSteps.testing().path(OFFER).path(mockOffer(type).getOfferId()).open();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeOpenPageTarget() {
        proxy.shouldSeeRequestInLog(allOf(containsString(AD_MAIL_RU),
                containsString(format("productid=%s", offerId)),
                containsString(format("list=%s", listParam)),
                containsString(format("pagetype=%s", OPEN_PAGE_PARAM))), equalTo(1));
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeAddToFavTarget() {
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onOfferCardPage().addToFavButton().click();
        proxy.shouldSeeRequestInLog(allOf(containsString(AD_MAIL_RU),
                containsString(format("productid=%s", offerId)),
                containsString(format("list=%s", listParam)),
                containsString(format("pagetype=%s", ADD_TO_FEV_PARAM))), equalTo(1));
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeShowPhoneTarget() {
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onOfferCardPage().offerCardSummary().showPhoneButton().click();
        proxy.shouldSeeRequestInLog(allOf(containsString(AD_MAIL_RU),
                containsString(format("productid=%s", offerId)),
                containsString(format("list=%s", listParam)),
                containsString(format("pagetype=%s", SHOW_PHONE_PARAM))), equalTo(1));
    }
}
