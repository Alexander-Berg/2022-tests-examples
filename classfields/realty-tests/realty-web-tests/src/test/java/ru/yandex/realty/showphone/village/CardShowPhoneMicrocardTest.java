package ru.yandex.realty.showphone.village;

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

import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.VILLAGE_CARD;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.VillageCardResponse.villageCardTemplate;
import static ru.yandex.realty.step.UrlSteps.ID_VALUE;

@DisplayName("Показ телефона. Карточка коттеджного поселка. Телефон в микрокарточке")
@Feature(VILLAGE_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CardShowPhoneMicrocardTest {

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
        basePageSteps.resize(1400, 1000);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в микрокарточке. Бесплатный поселок")
    public void shouldSeePhoneFreeVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageSitePage().villageCardAbout().showPhoneClick();
        basePageSteps.onVillageSitePage().villageCardAbout().shouldSeePhone(TEST_PHONE);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в микрокарточке. Платный поселок")
    public void shouldSeePhonePayedVillageNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageSitePage().villageCardAbout().showPhoneClick();
        basePageSteps.onVillageSitePage().villageCardAbout().shouldSeePhone(TEST_PHONE);
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Показать телефон» раскрвает все телефоны на карточке и в галерее")
    public void shouldSeeAllVillagePhones() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .villageCardStub(villageCardTemplate().setId(ID).build(), ID)
                .villageContactsStub(newbuildingContactResponse.build(), ID)
                .createWithDefaults();

        urlSteps.testing().villageSite().replaceQueryParam(ID_VALUE, ID).open();
        basePageSteps.onVillageSitePage().villageCardAbout().showPhoneClick();
        basePageSteps.scrollDown(1000);
        basePageSteps.onVillageSitePage().villageCardAbout().shouldSeePhone(TEST_PHONE);
        basePageSteps.onVillageSitePage().hideableBlock().shouldSeePhone(TEST_PHONE);
        basePageSteps.onVillageSitePage().cardDev().shouldSeePhone(TEST_PHONE);
        basePageSteps.onVillageSitePage().galleryPic().click();
        basePageSteps.onVillageSitePage().galleryAside().shouldSeePhone(TEST_PHONE);
    }
}