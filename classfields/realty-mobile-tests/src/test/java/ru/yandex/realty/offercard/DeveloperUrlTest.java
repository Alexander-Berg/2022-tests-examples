package ru.yandex.realty.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT_IN_SITE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@DisplayName("Ссылка на карточку застройщика")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1461")
public class DeveloperUrlTest {

    private static final String NAME = "Кекс";
    private static final String NAME_TRANSLIT = "keks";
    private static final String ID = "26500";

    private MockOffer offer;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на карточку застройщика")
    public void shouldSeeDeveloperUrl() {
        offer = mockOffer(SELL_APARTMENT_IN_SITE);
        mockRuleConfigurable.siteWithOffersStatStub(
                mockSiteWithOffersStatTemplate().setDeveloperId(ID).setDeveloperName(NAME).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).createWithDefaults();
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().devInfo().link());
        basePageSteps.onOfferCardPage().devInfo().link().should(hasHref(equalTo(
                urlSteps.testing().path(SPB_I_LO).developerPath(NAME_TRANSLIT, ID).toString())));
    }
}
