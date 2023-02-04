package ru.yandex.realty.showphone.newbuilding;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.mock.NewbuildingContactResponse;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.element.offercard.PhoneBlock.TEL_HREF_PATTERN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.page.NewBuildingCardPage.CALL;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplatePayedJk;
import static ru.yandex.realty.mock.SiteWithOffersStatResponse.mockSiteWithOffersStatTemplate;

@DisplayName("Показ телефона. Карточка новостройки. Телефон в галерее")
@Feature(NEWBUILDING_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class CardShowPhoneGalleryTest {

    public static final int NB_ID = 200200;
    private static final String TEST_PHONE = "+71112223344";
    private static final String SECOND_TEST_PHONE = "+72225556677";
    private static final int TEST_NEWBUILDING_ID = 2073614;
    private static final String TEST_NEWBUILDING_ID_PATH =
            format("/alhimovo-%s/", TEST_NEWBUILDING_ID);
    private static final String PHOTO = "фото";

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

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в галерее. Бесплатный ЖК")
    public void shouldSeePhoneFreeJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .siteWithOffersStatStub(mockSiteWithOffersStatTemplate().setNewbuildingId(NB_ID).build())
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.onNewBuildingCardPage().navbarShortcut(PHOTO).click();
        basePageSteps.onNewBuildingCardPage().gallery().link(CALL).click();
        basePageSteps.onNewBuildingCardPage().gallery().link(CALL)
                .should(hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в галерее. Платный ЖК")
    public void shouldSeePhonePayedJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplatePayedJk().addPhone(TEST_PHONE);
        mockRuleConfigurable
                .siteWithOffersStatStub(mockSiteWithOffersStatTemplate().setNewbuildingId(NB_ID).build())
                .newBuildingContacts(newbuildingContactResponse.build(), NB_ID)
                .createWithDefaults();

        urlSteps.testing().newbuildingSiteMock().open();
        basePageSteps.onNewBuildingCardPage().navbarShortcut(PHOTO).click();
        basePageSteps.onNewBuildingCardPage().gallery().link(CALL).click();
        basePageSteps.onNewBuildingCardPage().gallery().link(CALL)
                .should(hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE))));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в галерее. Спецпроект. Видим телефон из «phonesWithTag»")
    public void shouldSeePhoneSpecialJkNormalCase() {
        newbuildingContactResponse = newbuildingContactTemplateFreeJk()
                .addPhone(SECOND_TEST_PHONE)
                .addSpecialPhone(TEST_PHONE);
        mockRuleConfigurable
                .newBuildingContacts(newbuildingContactResponse.build(), TEST_NEWBUILDING_ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(TEST_NEWBUILDING_ID_PATH)
                .queryParam(UrlSteps.FROM_SPECIAL, UrlSteps.SAMOLET_VALUE).open();
        basePageSteps.onNewBuildingCardPage().navbarShortcut(PHOTO).click();
        basePageSteps.onNewBuildingCardPage().gallery().link(CALL).click();
        basePageSteps.onNewBuildingCardPage().gallery().link(CALL)
                .should(hasHref(equalTo(format(TEL_HREF_PATTERN, TEST_PHONE))));
    }
}
