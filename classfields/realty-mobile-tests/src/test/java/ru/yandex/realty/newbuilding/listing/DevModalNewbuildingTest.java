package ru.yandex.realty.newbuilding.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.beans.DeveloperSiteResponse.developer;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING;
import static ru.yandex.realty.mobile.element.listing.TouchSite.CALL;
import static ru.yandex.realty.mobile.element.newbuilding.DevModal.CANCEL;
import static ru.yandex.realty.mock.MockSite.SITE_TEMPLATE;
import static ru.yandex.realty.mock.MockSite.mockSite;
import static ru.yandex.realty.mock.OfferWithSiteSearchResponse.offerWithSiteSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.DEVELOPER_ID_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@Issue("VERTISTEST-1352")
@Epic(NEWBUILDING)
@Feature("Модалка телефонов застройщиков")
@DisplayName("Модалка телефонов застройщиков")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class DevModalNewbuildingTest {

    private static final String FIRST_DEVELOPER_NAME = "Первый застройщик";
    private static final String FIRST_DEVELOPER_PHONE = "+78124563225";
    private static final String SECOND_DEVELOPER_NAME = "Второй застройщик";
    private static final String SECOND_DEVELOPER_PHONE = "+78124531225";

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

    @Before
    public void before() {
        mockRuleConfigurable.offerWithSiteSearchStub(offerWithSiteSearchTemplate().sites(asList(
                mockSite(SITE_TEMPLATE).setDevelopers(
                        developer().setName(FIRST_DEVELOPER_NAME).setPhones(asList(FIRST_DEVELOPER_PHONE)),
                        developer().setName(SECOND_DEVELOPER_NAME).setPhones(asList(SECOND_DEVELOPER_PHONE))
                ))).buildSite()).createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).queryParam(DEVELOPER_ID_URL_PARAM, "26300").open();
        basePageSteps.onNewBuildingPage().site(FIRST).button(CALL).click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка телефонов застройщиков. Первый застройщик.")
    public void shouldSeePhoneFirstDev() {
        basePageSteps.onNewBuildingPage().devModal().devPhone(FIRST_DEVELOPER_NAME).should(
                hasText(makePhoneFormatted(FIRST_DEVELOPER_PHONE, PHONE_PATTERN_BRACKETS)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Модалка телефонов застройщиков. Второй застройщик.")
    public void shouldSeePhoneSecondDev() {
        basePageSteps.onNewBuildingPage().devModal().devPhone(SECOND_DEVELOPER_NAME).should(
                hasText(makePhoneFormatted(SECOND_DEVELOPER_PHONE, PHONE_PATTERN_BRACKETS)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Закрытие модалки телефонов застройщиков")
    public void shouldCloseDevModal() {
        basePageSteps.onNewBuildingPage().devModal().button(CANCEL).click();

        basePageSteps.onNewBuildingPage().devModal().should(not(isDisplayed()));
    }

}
