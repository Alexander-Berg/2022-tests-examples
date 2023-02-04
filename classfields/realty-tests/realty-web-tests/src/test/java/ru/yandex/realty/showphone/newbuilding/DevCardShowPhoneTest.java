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

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mock.MockDeveloper.BASIC_DEVELOPER;
import static ru.yandex.realty.mock.MockDeveloper.mockDeveloper;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_SPACES;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@DisplayName("Показ телефона. Карточка застройщика новостроек")
@Feature(NEWBUILDING_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DevCardShowPhoneTest {

    public static final int NB_ID = 200200;
    private static final String TEST_PHONE = "+71112223344";
    private static final String DEVELOPER_ID = "2000";
    private static final String DEVELOPER_PATH = "donstroj-21331";

    private NewbuildingContactResponse newbuildingContactResponse;
    private String developer;
    private String offerWithSiteSearchResponse;

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
        developer = mockDeveloper(BASIC_DEVELOPER).setId(DEVELOPER_ID).build();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона карточка застройщика. Бесплатный ЖК")
    public void shouldSeePhoneFreeJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .developerStub(DEVELOPER_ID, developer)
                .offerWithSiteSearchStub(offerWithSiteSearchResponse)
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(ZASTROYSCHIK).path(DEVELOPER_PATH).open();
        basePageSteps.onDeveloperPage().newBuildingSnippet(FIRST).showPhoneButton().click();
        basePageSteps.onDeveloperPage().newBuildingSnippet(FIRST).showPhoneButton()
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_SPACES)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона карточка застройщика. Платный ЖК")
    public void shouldSeePhonePayedJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .developerStub(DEVELOPER_ID, developer)
                .offerWithSiteSearchStub(offerWithSiteSearchResponse)
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(ZASTROYSCHIK).path(DEVELOPER_PATH).open();
        basePageSteps.onDeveloperPage().newBuildingSnippet(FIRST).showPhoneButton().click();
        basePageSteps.onDeveloperPage().newBuildingSnippet(FIRST).showPhoneButton()
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_SPACES)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ 500ки карточка застройщика")
    public void shouldSeePhone500() {
        mockRuleConfigurable
                .developerStub(DEVELOPER_ID, developer)
                .offerWithSiteSearchStub(offerWithSiteSearchResponse)
                .newBuildingContactsStub500(NB_ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(ZASTROYSCHIK).path(DEVELOPER_PATH).open();
        basePageSteps.onDeveloperPage().newBuildingSnippet(FIRST).showPhoneButton().click();
        basePageSteps.onDeveloperPage().newBuildingSnippet(FIRST).showPhoneButton().should(isDisplayed());
    }
}
