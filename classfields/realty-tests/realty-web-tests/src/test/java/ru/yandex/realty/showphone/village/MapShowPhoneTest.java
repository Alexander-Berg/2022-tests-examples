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

import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGES;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.VillageCardResponse.villageCardTemplate;
import static ru.yandex.realty.mock.VillagePointSearchTemplate.villagePointSearchTemplate;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_SPACES;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@DisplayName("Показ телефона. Карта поселков")
@Feature(VILLAGES)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MapShowPhoneTest {

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
    @DisplayName("Показ телефона карта новостроек. Бесплатный поселок")
    public void shouldSeePhoneFreeVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villagePointSearch(villagePointSearchTemplate().setId(ID).build())
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton().click();
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton()
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_SPACES)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона карта новостроек. Платный поселок")
    public void shouldSeePhonePayedVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villagePointSearch(villagePointSearchTemplate().setId(ID).build())
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton().click();
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton()
                .should(hasText(makePhoneFormatted(TEST_PHONE, PHONE_PATTERN_SPACES)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ 500ки карта поселков.")
    public void shouldSeePhone500() {
        mockRuleConfigurable
                .villagePointSearch(villagePointSearchTemplate().setId(ID).build())
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub500(ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton().click();
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton().should(isDisplayed());
    }

    @Ignore("МОЖЕТ ЛИ БЫТЬ ДВА ТЕЛЕФОНА?")
    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ двух телефонов карта поселков.")
    public void shouldSeePhoneFreeVillageTwoPhones() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE)
                .addPhone(SECOND_TEST_PHONE);
        mockRuleConfigurable
                .villagePointSearch(villagePointSearchTemplate().setId(ID).build())
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).path(KARTA).open();
        basePageSteps.moveCursorAndClick(basePageSteps.onMapPage().mapOffer(FIRST));
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton().click();
        basePageSteps.onMapPage().sidebar().villageMapOffer().showPhoneButton()
                .should(hasText(TEST_PHONE + SECOND_TEST_PHONE));
    }
}