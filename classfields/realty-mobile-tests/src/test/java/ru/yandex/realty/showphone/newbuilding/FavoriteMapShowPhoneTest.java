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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.NewbuildingContactResponse;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.FAVORITES;
import static ru.yandex.realty.consts.Pages.KARTA;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.element.offercard.PhoneBlock.TEL_HREF_PATTERN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.map.MapOffer.CALL;
import static ru.yandex.realty.mock.FavoritesResponse.favoritesTemplate;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.NewBuildingSimplePointSearchTemplate.newBuildingSimplePointSearchTemplate;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Показ телефона. Избранное карта")
@Feature(NEWBUILDING_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class FavoriteMapShowPhoneTest {

    public static final int NB_ID = 872687;
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
    @DisplayName("Показ телефона избранное карта. Бесплатный ЖК")
    public void shouldSeePhoneFreeJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .newbuildingSimplePointSearchStub(newBuildingSimplePointSearchTemplate().setId(NB_ID).build())
                .offerWithSiteSearchStub(offerWithSiteSearchResponse)
                .favoritesStub(favotitesResponse)
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));

        basePageSteps.onMobileMapPage().newBuildingMapOffer(FIRST).button(CALL).click();
        basePageSteps.onMobileMapPage().newBuildingMapOffer(FIRST).link(CALL)
                .should(hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона избранное карта. Платный ЖК")
    public void shouldSeePhonePayedJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .newbuildingSimplePointSearchStub(newBuildingSimplePointSearchTemplate().setId(NB_ID).build())
                .offerWithSiteSearchStub(offerWithSiteSearchResponse)
                .favoritesStub(favotitesResponse)
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));

        basePageSteps.onMobileMapPage().newBuildingMapOffer(FIRST).button(CALL).click();
        basePageSteps.onMobileMapPage().newBuildingMapOffer(FIRST).link(CALL)
                .should(hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ 500ки избранное")
    public void shouldSeePhone500() {
        mockRuleConfigurable
                .newbuildingSimplePointSearchStub(newBuildingSimplePointSearchTemplate().setId(NB_ID).build())
                .offerWithSiteSearchStub(offerWithSiteSearchResponse)
                .favoritesStub(favotitesResponse)
                .newBuildingContactsStub500(NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(FAVORITES).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMapPage().pin(FIRST));

        basePageSteps.onMobileMapPage().newBuildingMapOffer(FIRST).button(CALL).click();
        basePageSteps.acceptAlert();
        basePageSteps.onMobileMapPage().newBuildingMapOffer(FIRST).button(CALL).should(isDisplayed());
    }
}
