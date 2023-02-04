package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@DisplayName("Иконка добавления в избранное не отображается на сниппете владельца оффера в блоке похожих")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class OfferCardNoFavoriteButtonOwnerSimilarBlockTest {

    private static final String CARD_ID = "1111111";

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
        urlSteps.testing().path(CARD).path(CARD_ID);

        mockRule.graphqlStub(mockResponse().setCard(
                        mockCard(BASIC_CARD).setId(CARD_ID).similarOffers(asList(
                                mockSnippet(BASIC_SNIPPET).getMockSnippet().setIsOwner(true),
                                mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                                mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                                mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                                mockSnippet(BASIC_SNIPPET).getMockSnippet())).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();
        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature("Блок похожих снизу")
    @DisplayName("Иконка добавления в избранное не отображается на сниппете владельца оффера в блоке похожих снизу")
    public void shouldNotSeeFavoriteButtonOwnerBottomSimilarBlock() {
        basePageSteps.onListingPage().snippetFirst().waitUntil(isDisplayed()).hover();

        basePageSteps.onListingPage().firstSnippet().addToFavorite().should(not(isDisplayed()));
    }

    @Test
    @Ignore("CLASSFRONT-1929")
    @Owner(ALEKS_IVANOV)
    @Feature("Блок похожих сверху")
    @DisplayName("Иконка добавления в избранное не отображается на сниппете владельца оффера в блоке похожих сверху")
    public void shouldNotSeeFavoriteButtonOwnerTopSimilarBlock() {
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();

        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).favorite().should(not(isDisplayed()));
    }

}
