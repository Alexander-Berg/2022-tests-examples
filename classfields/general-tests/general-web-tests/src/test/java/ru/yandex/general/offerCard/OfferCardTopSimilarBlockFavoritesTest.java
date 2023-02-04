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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.general.beans.ajaxRequests.AddToFavorites.addToFavorites;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Notifications.ADDED_TO_FAV;
import static ru.yandex.general.consts.Notifications.OFFER_DELETED_FROM_FAV;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockListingSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockListingSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.AjaxProxySteps.ADD_TO_FAVORITES;
import static ru.yandex.general.step.AjaxProxySteps.DELETE_FROM_FAVORITES;
import static ru.yandex.general.utils.Utils.getRandomOfferId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Блок похожих сверху")
@DisplayName("Добавление/удаление в избранное из блока похожих сверху")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class OfferCardTopSimilarBlockFavoritesTest {

    private static final String ID = "12345";
    String similarId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        similarId = getRandomOfferId();
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(CARD).path(ID);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем в избранное из блока похожих сверху, проверяем запрос")
    public void shouldSeeAddToFavoritesFromTopSimilarBlock() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setId(similarId),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet())).build())
                .setCategoriesTemplate().setRegionsTemplate().setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();
        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).favorite().click();
        basePageSteps.onOfferCardPage().popupNotification(ADDED_TO_FAV).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(ADD_TO_FAVORITES).withRequestText(
                addToFavorites().setOfferIds(asList(similarId))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем из избранного из блока похожих сверху, проверяем запрос")
    public void shouldRemoveFavoritesFromTopSimilarBlock() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).similarOffers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setId(similarId).setFavorite(true),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet(),
                        mockSnippet(BASIC_SNIPPET).getMockSnippet())).build())
                .setCategoriesTemplate().setRegionsTemplate().setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();
        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).favorite().click();
        basePageSteps.onOfferCardPage().popupNotification(OFFER_DELETED_FROM_FAV).waitUntil(isDisplayed());

        ajaxProxySteps.setAjaxHandler(DELETE_FROM_FAVORITES).withRequestText(
                addToFavorites().setOfferIds(asList(similarId))).shouldExist();
    }

}
