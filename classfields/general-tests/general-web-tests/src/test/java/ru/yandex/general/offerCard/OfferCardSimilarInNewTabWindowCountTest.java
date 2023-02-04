package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Кол-во окон после открытия оффера их похожих = «2»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardSimilarInNewTabWindowCountTest {

    private static final String ID = "12345";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).addSimilarOffers(6).build())
                .setCategoriesTemplate().setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кол-во окон после открытия оффера из списка похожих снизу = «2»")
    public void shouldSeeWindowCountAfterOpenOfferCardSimilarBottom() {
        basePageSteps.onOfferCardPage().similarSnippetFirst().click();

        assertThat("Кол-во окон после открытия оффера = «2»",
                basePageSteps.getWindowCount(), is(2));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кол-во окон после открытия оффера из списка похожих сверху = «2»")
    public void shouldSeeWindowCountAfterOpenOfferCardSimilarTop() {
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();
        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).waitUntil(isDisplayed()).click();

        assertThat("Кол-во окон после открытия оффера = «2»",
                basePageSteps.getWindowCount(), is(2));
    }

}
