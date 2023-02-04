package ru.yandex.realty.showphone.village;

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

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;
import static ru.yandex.realty.mock.MockVillage.VILLAGE_COTTAGE;
import static ru.yandex.realty.mock.MockVillage.mockVillage;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.VillageCardResponse.villageCardTemplate;
import static ru.yandex.realty.mock.VillageSearchResponse.villageSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.ID_VALUE;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_SPACES;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@DisplayName("Показ телефона. Объекты от застройщика поселка")
@Feature(VILLAGE_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class FromDevShowPhoneTest {

    public static final String ID = "200200";
    private static final String TEST_PHONE = "+71112223344";
    private static final String SECOND_TEST_PHONE = "+72225556677";

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
        basePageSteps.resize(1400, 1600);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона объекты от застройщика. Бесплатный поселок")
    public void shouldSeePhoneFreeVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageSearchStub(villageSearchTemplate().villages(asList(
                        mockVillage(VILLAGE_COTTAGE).setId(ID))).build())
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageSitePage().fromDev(FIRST).showPhoneButton().click();
        basePageSteps.onVillageSitePage().fromDev(FIRST).showPhoneButton()
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_SPACES)));

    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона объекты от застройщика. Платный поселок")
    public void shouldSeePhonePayedVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageSearchStub(villageSearchTemplate().villages(asList(
                        mockVillage(VILLAGE_COTTAGE).setId(ID))).build())
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageSitePage().fromDev(FIRST).showPhoneButton().click();
        basePageSteps.onVillageSitePage().fromDev(FIRST).showPhoneButton()
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_SPACES)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ 500ки объекты от застройщика.")
    public void shouldSeePhone500() {
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageSearchStub(villageSearchTemplate().villages(asList(
                        mockVillage(VILLAGE_COTTAGE).setId(ID))).build())
                .villageContactsStub500(ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageSitePage().fromDev(FIRST).showPhoneButton().click();
        basePageSteps.onVillageSitePage().fromDev(FIRST).showPhoneButton().should(isDisplayed());
    }

    @Ignore("МОЖЕТ ЛИ БЫТЬ ДВА ТЕЛЕФОНА?")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ двух телефонов объекты от застройщика.")
    public void shouldSeePhoneFreeVillageTwoPhones() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE)
                .addPhone(SECOND_TEST_PHONE);
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageSearchStub(villageSearchTemplate().villages(asList(
                        mockVillage(VILLAGE_COTTAGE).setId(ID))).build())
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageSitePage().fromDev(FIRST).showPhoneButton().click();
        basePageSteps.onVillageSitePage().fromDev(FIRST).showPhoneButton()
                .should(hasText(TEST_PHONE + SECOND_TEST_PHONE));
    }
}