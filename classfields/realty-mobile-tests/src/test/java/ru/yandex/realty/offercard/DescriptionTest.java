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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.RENT_COMMERCIAL_WAREHOUSE;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class DescriptionTest {

    public static final String FULL_DESCRIPTION = "Полное описание";
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

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        offer = mockOffer(RENT_COMMERCIAL_WAREHOUSE);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).createWithDefaults();
        compareSteps.resize(385, 10000);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликаем на «Полное описание»")
    public void shouldSeeFullDescription() {
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onOfferCardPage().descriptionText());
        int initialLength = basePageSteps.onOfferCardPage().descriptionText().getText().length();
        basePageSteps.onOfferCardPage().spanLink(FULL_DESCRIPTION).click();
        basePageSteps.onOfferCardPage().spanLink(FULL_DESCRIPTION).waitUntil(not(exists()));
        int finalLength = basePageSteps.onOfferCardPage().descriptionText().getText().length();
        assertThat(finalLength).describedAs("Длинна текста описания").isGreaterThan(initialLength);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот не полного описания")
    public void shouldSeeShortDescriptionScreenShot() {
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().descriptionText());
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().descriptionText());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот полного описания")
    public void shouldSeeFullDescriptionScreenShot() {
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollToElement(basePageSteps.onOfferCardPage().spanLink(FULL_DESCRIPTION));
        basePageSteps.onOfferCardPage().spanLink(FULL_DESCRIPTION).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().descriptionText());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.scrollToElement(basePageSteps.onOfferCardPage().spanLink(FULL_DESCRIPTION));
        basePageSteps.onOfferCardPage().spanLink(FULL_DESCRIPTION).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().descriptionText());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
