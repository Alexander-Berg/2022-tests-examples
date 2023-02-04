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

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.NEWBUILDING_CARD;
import static ru.yandex.realty.mock.NewbuildingContactResponse.newbuildingContactTemplateFreeJk;
import static ru.yandex.realty.step.UrlSteps.FROM_SPECIAL;
import static ru.yandex.realty.step.UrlSteps.SAMOLET_VALUE;

@DisplayName("Показ телефона. Карточка новостройки. Спецпроект")
@Feature(NEWBUILDING_CARD)
@Link("https://st.yandex-team.ru/VERTISTEST-1600")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CardShowPhoneSpecProjectTest {

    private static final String TEST_PHONE = "+71112223344";
    private static final String SECOND_TEST_PHONE = "+72225556677";
    private static final int TEST_NEWBUILDING_ID = 2651324;
    private static final String TEST_NEWBUILDING_ID_PATH =
            format("/pribrezhnyj-park-%s/", TEST_NEWBUILDING_ID);

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
        newbuildingContactResponse = newbuildingContactTemplateFreeJk()
                .addPhone(SECOND_TEST_PHONE)
                .addSpecialPhone(TEST_PHONE);
        mockRuleConfigurable
                .newBuildingContacts(newbuildingContactResponse.build(), TEST_NEWBUILDING_ID)
                .createWithDefaults();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(TEST_NEWBUILDING_ID_PATH)
                .queryParam(FROM_SPECIAL, SAMOLET_VALUE).queryParam("tag", "special").open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в плавающем блоке. Спецпроект. Видим телефон из «phonesWithTag»")
    public void shouldSeePhoneSpecialJkHideable() {
        basePageSteps.scrollDown(1000);
        basePageSteps.onNewBuildingSpecSitePage().hideableBlock().showPhoneClick();
        basePageSteps.onNewBuildingSpecSitePage().hideableBlock().shouldSeePhone(TEST_PHONE);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в галерее. Спецпроект. Видим телефон из «phonesWithTag»")
    public void shouldSeePhoneSpecialJkGallery() {
        basePageSteps.onNewBuildingSpecSitePage().photoShowButton().click();
        basePageSteps.onNewBuildingSpecSitePage().galleryAside().showPhoneClick();
        basePageSteps.onNewBuildingSpecSitePage().galleryAside().shouldSeePhone(TEST_PHONE);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в хедере карточки. Спецпроект. Видим телефон из «phonesWithTag»")
    public void shouldSeePhoneSpecialJkMHeader() {
        basePageSteps.onNewBuildingSpecSitePage().siteCardSecondPackageHeader().showPhoneButton().click();
        basePageSteps.onNewBuildingSpecSitePage().siteCardSecondPackageHeader().showPhoneButton()
                .should(allOf(hasClass(containsString("CardPhone")), hasClass(containsString("_shown"))));
        basePageSteps.onNewBuildingSpecSitePage().siteCardSecondPackageHeader().shouldSeePhone(TEST_PHONE);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показ телефона в карточке застройщика. Спецпроект. Видим телефон из «phonesWithTag»")
    public void shouldSeePhoneSpecialJkCardDev() {
        basePageSteps.onNewBuildingSpecSitePage().cardDev().showPhoneClick();
        basePageSteps.onNewBuildingSpecSitePage().cardDev().shouldSeePhone(TEST_PHONE);
    }
}