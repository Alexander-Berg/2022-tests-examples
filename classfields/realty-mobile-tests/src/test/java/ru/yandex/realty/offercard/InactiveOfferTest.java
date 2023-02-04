package ru.yandex.realty.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.TRYOHKOMNATNAYA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;
import static ru.yandex.realty.step.UrlSteps.RGID;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class InactiveOfferTest {

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

    @Before
    public void before() {
        offer = mockOffer(SELL_APARTMENT).setObsolete(now().minusDays(1));
        mockRuleConfigurable.cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build())
                .createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("В карточке оффера есть блок «Объявление устарело»")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeOfferCardOverListing() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().inactiveBlock()
                .should(hasText(containsString("Объявление устарело или снято с публикации")));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Посмотреть похожие»")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeSimilarFromInactive() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().button("Посмотреть похожие").click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(TRYOHKOMNATNAYA)
                .path("/dist-chertanovo-centralnoe-193336/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик по «Искать новые»")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeNewFromInactive() {
        urlSteps.testing().path(Pages.OFFER).path(offer.getOfferId()).open();
        basePageSteps.onOfferCardPage().button("Искать новые").click();
        urlSteps.testing().path(MOSKVA_I_MO).ignoreParam(RGID).shouldNotDiffWithWebDriverUrl();
    }
}
