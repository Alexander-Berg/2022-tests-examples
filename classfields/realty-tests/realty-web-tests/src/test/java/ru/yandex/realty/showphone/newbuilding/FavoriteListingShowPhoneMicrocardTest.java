package ru.yandex.realty.showphone.newbuilding;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mock.NewbuildingContactResponse;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.FAVORITES;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.element.saleads.ActionBar.SHOW_PHONE;
import static ru.yandex.realty.mock.FavoritesResponse.favoritesTemplate;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.FAVORITE_TYPE_PARAM;
import static ru.yandex.realty.step.UrlSteps.SITE_URL_VALUE;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@DisplayName("Показ телефона. Избранное")
@Feature(NEWBUILDING_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class FavoriteListingShowPhoneMicrocardTest {

    public static final int NB_ID = 200200;
    private static final String TEST_PHONE = "+71112223344";

    private NewbuildingContactResponse newbuildingContactResponse;
    private String offerWithSiteSearchResponse;
    private String favotitesResponse;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        offerWithSiteSearchResponse = offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE).setId(NB_ID))).buildSite();
        favotitesResponse = favoritesTemplate().addItem(format("site_%s", NB_ID)).build();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона иззбранное. Бесплатный ЖК")
    public void shouldSeePhoneFreeJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchResponse)
                .favoritesStub(favotitesResponse)
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).queryParam(FAVORITE_TYPE_PARAM, SITE_URL_VALUE).open();
        basePageSteps.onFavoritesPage().offer(FIRST).buttonWithTitle(SHOW_PHONE).click();
        basePageSteps.onFavoritesPage().offer(FIRST).buttonWithTitle(SHOW_PHONE)
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_BRACKETS)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона иззбранное. Платный ЖК")
    public void shouldSeePhonePayedJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchResponse)
                .favoritesStub(favotitesResponse)
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).queryParam(FAVORITE_TYPE_PARAM, SITE_URL_VALUE).open();
        basePageSteps.onFavoritesPage().offer(FIRST).buttonWithTitle(SHOW_PHONE).click();
        basePageSteps.onFavoritesPage().offer(FIRST).buttonWithTitle(SHOW_PHONE)
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_BRACKETS)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ 500ки иззбранное")
    public void shouldSeePhone500() {
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchResponse)
                .favoritesStub(favotitesResponse)
                .newBuildingContactsStub500(NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).queryParam(FAVORITE_TYPE_PARAM, SITE_URL_VALUE).open();
        basePageSteps.onFavoritesPage().offer(FIRST).buttonWithTitle(SHOW_PHONE).click();
        basePageSteps.onFavoritesPage().offer(FIRST).buttonWithTitle(SHOW_PHONE).should(isDisplayed());
    }
}