package ru.yandex.general.offerCard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.CardStatus.BANNED;
import static ru.yandex.general.consts.CardStatus.OfferBanReasons.SPAM;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.categoryListingExample;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Редирект с забаненного оффера")
@DisplayName("Редирект с забаненного оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class OfferCardBannedRedirectTest {

    private static final String ID = "12345";
    private static final String CATEGORY_URL = "/moskva/elektronika/mobilnie-telefoni/";

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

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Редирект на листинг, при переходе на забаненный оффер покупателем")
    public void shouldSeeRedirectListingFromBannedOfferBuyer() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM).setIsOwner(false).setCategoryUrl(CATEGORY_URL).build())
                .setSearch(categoryListingExample().build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();

        urlSteps.testing().path(CATEGORY_URL).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет редиректа на листинг, при переходе на забаненный оффер продавцом")
    public void shouldSeeNoRedirectListingFromBannedOfferOwner() {
        mockRule.graphqlStub(mockResponse().setCard(mockCard(BASIC_CARD).setStatus(BANNED)
                        .setBanReasons(SPAM).setIsOwner(true).setCategoryUrl(CATEGORY_URL).build())
                .setCategoriesTemplate().setRegionsTemplate().build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).path(SLASH).open();
        basePageSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

}
