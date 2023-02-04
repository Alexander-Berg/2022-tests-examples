package ru.yandex.realty.offercard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.MockOffer;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.OFFER;
import static ru.yandex.realty.consts.RealtyFeatures.OFFER_CARD;
import static ru.yandex.realty.mock.CardMockResponse.cardTemplate;
import static ru.yandex.realty.mock.CardWithViewsResponse.cardWithViewsTemplate;
import static ru.yandex.realty.mock.MockOffer.SELL_APARTMENT;
import static ru.yandex.realty.mock.MockOffer.mockOffer;

@DisplayName("Карточка оффера")
@Feature(OFFER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
@Issue("VERTISTEST-1351")
public class FullFoldedExpandableBlocksTest {

    public static final String FULL_DESCRIPTION = "Полное описание";
    // TODO: add "Данные из ЕГРН"
    private static final List<String> SECTIONS = asList("Характеристики", "Описание", "О доме", "О районе",
            "История объявлений в этом доме");
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
        offer = mockOffer(SELL_APARTMENT);
        mockRuleConfigurable
                .cardStub(cardTemplate().offers(asList(offer)).build())
                .cardWithViewsStub(cardWithViewsTemplate().offer(offer).build()).createWithDefaults();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот со свернутыми карточками")
    @Description("ПОЧЕМУ РЕКЛАМА ЯНДЕКС ТАКСИ РАЗНАЯ")
    @Category({Regression.class, Mobile.class})
    public void shouldSeeFoldedCardsScreenShot() {
        compareSteps.resize(385, 5000);
        urlSteps.testing().path(OFFER).path(offer.getOfferId()).open();
        basePageSteps.scrollDown(1000);
        foldSections();

        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().pageRoot());
        urlSteps.setMobileProductionHost().open();
        basePageSteps.scrollDown(1000);
        foldSections();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().pageRoot());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Step("Скрываем все секции")
    private void foldSections() {
        SECTIONS.forEach(section -> await().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .alias(format("Ждем пока свернется «%s»", section)).ignoreExceptions()
                .pollInterval(1, SECONDS).atMost(30000, MILLISECONDS)
                .until(() -> {
                    basePageSteps.onOfferCardPage().cardSection(section).header().click();
                    basePageSteps.onOfferCardPage().cardSection(section).content()
                            .should(hasAttribute("aria-hidden", "true"));
                    return true;
                }));
    }
}
