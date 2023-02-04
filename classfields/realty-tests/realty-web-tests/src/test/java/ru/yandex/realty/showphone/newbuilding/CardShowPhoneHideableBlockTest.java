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

import static java.lang.Integer.parseInt;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@DisplayName("Показ телефона. Карточка новостройки. Телефон в плавающем блоке")
@Feature(NEWBUILDING_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CardShowPhoneHideableBlockTest {

    public static final int NB_ID = parseInt(MockRuleConfigurable.NB_ID);
    private static final String TEST_PHONE = "+71112223344";

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
        basePageSteps.resize(1400, 1500);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в плавающем блоке. Бесплатный ЖК")
    public void shouldSeePhoneFreeJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .siteWithOffersStatStub(mockSiteWithOffersStatTemplate().setNewbuildingId(NB_ID).build())
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrolling(800, 100);
        basePageSteps.onNewBuildingSitePage().hideableBlock().showPhoneClick();
        basePageSteps.onNewBuildingSitePage().hideableBlock().shouldSeePhone(TEST_PHONE);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в плавающем блоке. Платный ЖК")
    public void shouldSeePhonePayedJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .siteWithOffersStatStub(mockSiteWithOffersStatTemplate().setNewbuildingId(NB_ID).build())
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrolling(800, 100);
        basePageSteps.onNewBuildingSitePage().hideableBlock().showPhoneClick();
        basePageSteps.onNewBuildingSitePage().hideableBlock().shouldSeePhone(TEST_PHONE);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ 500ки в плавающем блоке.")
    public void shouldSeePhone500() {
        mockRuleConfigurable
                .siteWithOffersStatStub(mockSiteWithOffersStatTemplate().setNewbuildingId(NB_ID).build())
                .newBuildingContactsStub500(NB_ID)
                .createWithDefaults();

        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrolling(800, 100);
        basePageSteps.onNewBuildingSitePage().hideableBlock().showPhoneButton().click();
        basePageSteps.onNewBuildingSitePage().hideableBlock().showPhoneButton().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на «Показать телефон» раскрывает все телефоны на карточке и в галерее")
    public void shouldSeeAllPhones() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .siteWithOffersStatStub(mockSiteWithOffersStatTemplate().setNewbuildingId(NB_ID).build())
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.scrolling(800, 100);
        basePageSteps.onNewBuildingSitePage().hideableBlock().showPhoneClick();
        basePageSteps.onNewBuildingSitePage().hideableBlock().shouldSeePhone(TEST_PHONE);
        basePageSteps.onNewBuildingSitePage().siteCardAbout().shouldSeePhone(TEST_PHONE);
        basePageSteps.onNewBuildingSitePage().cardDev().shouldSeePhone(TEST_PHONE);
        basePageSteps.onNewBuildingSitePage().galleryPic().click();
        basePageSteps.onNewBuildingSitePage().galleryAside().shouldSeePhone(TEST_PHONE);
    }
}
