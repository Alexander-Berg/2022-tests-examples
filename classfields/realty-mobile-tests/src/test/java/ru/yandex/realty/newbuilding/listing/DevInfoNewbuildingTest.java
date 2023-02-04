package ru.yandex.realty.newbuilding.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;
import static ru.yandex.realty.beans.developerSearchQuery.DeveloperSearchQueryResponse.developerByTemplate;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.DEVELOPER_INFO;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.newbuilding.DevInfo.FINISHED;
import static ru.yandex.realty.mobile.element.newbuilding.DevInfo.MORE_ABOUT_DEVELOPER;
import static ru.yandex.realty.mobile.element.newbuilding.DevInfo.SUSPENDED;
import static ru.yandex.realty.mobile.element.newbuilding.DevInfo.UNFINISHED;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.UrlSteps.DEVELOPER_ID_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@Issue("VERTISTEST-1352")
@Epic(NEWBUILDING)
@Feature(DEVELOPER_INFO)
@DisplayName("Информация о застройщике")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class DevInfoNewbuildingTest {

    private static final String NAME = "Кекс";
    private static final String NAME_TRANSLIT = "keks";
    private static final String LOGO = "https://avatars.mdst.yandex.net/get-realty/3274/company.151019.5760737860850953430/builder_logo_info";
    private static final String BORN_YEAR = "1994";
    private static final int COUNT_HOUSES = 123;
    private static final String ID = "26500";

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

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Имя застройщика")
    public void shouldSeeDevName() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).setDeveloper(developerByTemplate().setName(NAME)).buildSite())
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(DEVELOPER_ID_URL_PARAM, "26300").open();

        basePageSteps.onNewBuildingPage().devInfo().name().should(hasText(String.format("Застройщик %s", NAME)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Лого застройщика")
    public void shouldSeeDevLogo() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).setDeveloper(developerByTemplate().setLogo(LOGO)).buildSite())
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(DEVELOPER_ID_URL_PARAM, "26300").open();

        basePageSteps.onNewBuildingPage().devInfo().logo().should(hasAttribute("src", LOGO));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Год появления застройщика")
    public void shouldSeeDevBorn() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).setDeveloper(developerByTemplate()
                .setBorn(String.format("%s-01-01T00:00:00Z", BORN_YEAR))).buildSite()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(DEVELOPER_ID_URL_PARAM, "26300").open();

        basePageSteps.onNewBuildingPage().devInfo().born().should(
                hasText(String.format("строит дома с %s года", BORN_YEAR)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Количество сданных домов")
    public void shouldSeeDevFinishedHouses() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).setDeveloper(developerByTemplate().withFinishedCount(COUNT_HOUSES))
                .buildSite()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(DEVELOPER_ID_URL_PARAM, "26300").open();

        basePageSteps.onNewBuildingPage().devInfo().amount(FINISHED).should(hasText(String.valueOf(COUNT_HOUSES)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Количество замороженных домов")
    public void shouldSeeDevSuspendedHouses() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).setDeveloper(developerByTemplate().withSuspendedCount(COUNT_HOUSES))
                .buildSite()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(DEVELOPER_ID_URL_PARAM, "26300").open();

        basePageSteps.onNewBuildingPage().devInfo().amount(SUSPENDED).should(hasText(String.valueOf(COUNT_HOUSES)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Количество строящихся домов")
    public void shouldSeeDevUnfinishedHouses() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).setDeveloper(developerByTemplate().withUnfinishedCount(COUNT_HOUSES))
                .buildSite()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(DEVELOPER_ID_URL_PARAM, "26300").open();

        basePageSteps.onNewBuildingPage().devInfo().amount(UNFINISHED).should(hasText(String.valueOf(COUNT_HOUSES)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка на карточку застройщика")
    public void shouldSeeDeveloperUrl() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE))).setDeveloper(developerByTemplate().setId(ID).setName(NAME))
                .buildSite()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(DEVELOPER_ID_URL_PARAM, "26300").open();

        basePageSteps.onNewBuildingPage().devInfo().link(MORE_ABOUT_DEVELOPER).should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA_I_MO).developerPath(NAME_TRANSLIT, ID).toString())));
    }

}
