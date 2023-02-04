package ru.yandex.realty.showphone.newbuilding;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
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
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_SPACES;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@DisplayName("Показ телефона. Карта новостроек")
@Feature(NEWBUILDING_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MapShowPhoneTest {

    public static final int NB_ID = 200200;
    private static final String TEST_PHONE = "+71112223344";
    private static final String SECOND_TEST_PHONE = "+72225556677";
    private static final int TEST_NEWBUILDING_ID = 375274;
    private static final String TEST_NEWBUILDING_ID_PATH =
            format("/salarevo-park-%s/", TEST_NEWBUILDING_ID);

    private NewbuildingContactResponse newbuildingContactResponse;

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
        basePageSteps.resize(1400, 1000);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона карта новостроек. Бесплатный ЖК")
    public void shouldSeePhoneFreeJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                        mockSite(SITE_TEMPLATE).setId(NB_ID))).buildSite())
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().click();
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton()
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_SPACES)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона карта новостроек. Платный ЖК")
    public void shouldSeePhonePayedJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                        mockSite(SITE_TEMPLATE).setId(NB_ID))).buildSite())
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().click();
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton()
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_SPACES)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ 500ки карта новостроек.")
    public void shouldSeePhone500() {
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                        mockSite(SITE_TEMPLATE).setId(NB_ID))).buildSite())
                .newBuildingContactsStub500(NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().click();
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().should(isDisplayed());
    }

    @Ignore("Два телефона не показываются")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ двух телефонов карта новостроек.")
    public void shouldSeePhoneFreeJkTwoPhones() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE)
                .addPhone(SECOND_TEST_PHONE);
        mockRuleConfigurable
                .offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                        mockSite(SITE_TEMPLATE).setId(NB_ID))).buildSite())
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().click();
        basePageSteps.onMapPage().sidebar().newbuildingCard().showPhoneButton().should(isDisplayed());
    }
}