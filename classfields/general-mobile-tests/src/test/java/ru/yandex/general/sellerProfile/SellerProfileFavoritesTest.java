package ru.yandex.general.sellerProfile;

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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.AjaxProxySteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.yandex.general.beans.ajaxRequests.AddToFavorites.addToFavorites;
import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.mock.MockPublicProfile.profileResponse;
import static ru.yandex.general.mock.MockPublicProfileSnippet.PROFILE_BASIC_SNIPPET;
import static ru.yandex.general.mock.MockPublicProfileSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.AjaxProxySteps.ADD_TO_FAVORITES;
import static ru.yandex.general.step.AjaxProxySteps.DELETE_FROM_FAVORITES;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(PROFILE_FEATURE)
@Feature("Добавление в избранное со страницы профиля")
@DisplayName("Добавление в избранное со страницы профиля")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class SellerProfileFavoritesTest {

    private static final String PUBLIC_ID = "3qpdmk0crc5xn1cu9qvam5r01c";
    private static final String OFFER_ID = "14125124124312";

    private MockResponse mockResponse = mockResponse()
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();

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

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private AjaxProxySteps ajaxProxySteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        passportSteps.commonAccountLogin();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Иконка добавления в избранное не отображается на сниппете владельца оффера в публичном профиле")
    public void shouldNotSeeFavoriteButtonOwner() {
        urlSteps.testing().path(PROFILE).path(PUBLIC_ID);
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().setPublicId(PUBLIC_ID).snippets(asList(
                        mockSnippet(PROFILE_BASIC_SNIPPET)
                )).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onProfilePage().firstSnippet().addToFavorite().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем сниппет в избранное на публичном профиле")
    public void shouldSeeAddToFavoritesRequest() {
        urlSteps.testing().path(PROFILE).path(SELLER_PATH);
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().setPublicId(PUBLIC_ID).snippets(asList(
                        mockSnippet(PROFILE_BASIC_SNIPPET).setId(OFFER_ID)
                )).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onProfilePage().firstSnippet().addToFavorite().click();

        ajaxProxySteps.setAjaxHandler(ADD_TO_FAVORITES).withRequestText(
                addToFavorites().setOfferIds(asList(OFFER_ID))).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем сниппет из избранного на публичном профиле")
    public void shouldSeeDeleteFromFavoritesRequest() {
        urlSteps.testing().path(PROFILE).path(SELLER_PATH);
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().setPublicId(PUBLIC_ID).snippets(asList(
                        mockSnippet(PROFILE_BASIC_SNIPPET).setId(OFFER_ID)
                )).build())
                .build()).withDefaults().create();
        urlSteps.open();
        basePageSteps.onProfilePage().firstSnippet().hover();
        basePageSteps.onProfilePage().firstSnippet().addToFavorite().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onProfilePage().firstSnippet().addToFavorite().click();

        ajaxProxySteps.setAjaxHandler(DELETE_FROM_FAVORITES).withRequestText(
                addToFavorites().setOfferIds(asList(OFFER_ID))).shouldExist();
    }

}
