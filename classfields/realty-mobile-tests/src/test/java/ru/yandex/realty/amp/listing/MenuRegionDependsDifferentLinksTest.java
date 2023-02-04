package ru.yandex.realty.amp.listing;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MAGADAN;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.OMSK;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.AMP;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.AMP_FEATURE;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.step.UrlSteps.FROM_PARAM;
import static ru.yandex.realty.step.UrlSteps.MAIN_MENU_VALUE;

@Link("VERTISTEST-1618")
@Feature(AMP_FEATURE)
@DisplayName("amp. Регионально зависимые ссылки в меню")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class MenuRegionDependsDifferentLinksTest {

    private static final String KVARTIRI_PIK = "Квартиры ПИК";
    private static final String KOTTEDZHNYE_POSELKI = "Коттеджи";
    private static final String CHOOSE_AREA_FOR_LIVE = "Выбери район для жизни";
    private static final String NOVOSTROJKI = "Новостройки";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. «Выбери район для жизни» нет в Магадане")
    public void shouldNotSeeChoiseAreaForLiveMagadanAmp() {
        urlSteps.testing().path(AMP).path(MAGADAN).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(CHOOSE_AREA_FOR_LIVE).should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Ссылка «Выбери район для жизни» в Санкт-Петербурге")
    public void shouldSeeChoiceAreaForLiveSanktPeterburgAmp() {
        urlSteps.testing().path(AMP).path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(CHOOSE_AREA_FOR_LIVE).should(hasHref(equalTo(urlSteps.testing()
                .uri("/sankt-peterburg/kupit/kvartira/karta/?isHeatmapsExpanded=true").toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Ссылка «Выбери район для жизни» в Москве")
    public void shouldSeeChoiceAreaForLiveMoskvaAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(CHOOSE_AREA_FOR_LIVE).should(hasHref(equalTo(urlSteps.testing()
                .uri("/moskva/kupit/kvartira/karta/?isHeatmapsExpanded=true").toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Коттеджных поселков нет в Магадане")
    public void shouldNotSeeVillagesMagadanAmp() {
        urlSteps.testing().path(AMP).path(MAGADAN).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(KOTTEDZHNYE_POSELKI).should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Ссылка на коттеджные поселки в Санкт-Петербурге")
    public void shouldSeeVillagesSanktPeterburgAmp() {
        urlSteps.testing().path(AMP).path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(KOTTEDZHNYE_POSELKI).should(hasHref(equalTo(urlSteps.testing()
                .uri("/sankt-peterburg/kupit/kottedzhnye-poselki/").toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Ссылка на коттеджные поселки в Москве")
    public void shouldSeeVillagesMoskvaAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(KOTTEDZHNYE_POSELKI).should(hasHref(equalTo(urlSteps.testing()
                .uri("/moskva/kupit/kottedzhnye-poselki/").toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Квартир от ПИК нет в Магадане")
    public void shouldNotSeePikMagadanAmp() {
        urlSteps.testing().path(AMP).path(MAGADAN).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(KVARTIRI_PIK).should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Квартир от ПИК нет в СПБ")
    public void shouldNotSeePikSanktPeterburgAmp() {
        urlSteps.testing().path(AMP).path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(KVARTIRI_PIK).should(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Ссылка на квартиры от Самолет в Москве")
    public void shouldSeePikUrlMoskvaAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link("Квартиры от Самолет").should(hasHref(equalTo(
                urlSteps.testing().path(SAMOLET).toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Ссылка на новостройки в Москве")
    public void shouldSeeNovostrojkiUrlMoskvaAmp() {
        urlSteps.testing().path(AMP).path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(NOVOSTROJKI).should(hasHref(equalTo(urlSteps.testing().path(AMP)
                .path("/moskva_i_moskovskaya_oblast/kupit/novostrojka/").toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Ссылка на новостройки в Санкт-Петербурге")
    public void shouldSeeNovostrojkiUrlSanktPeterburgAmp() {
        urlSteps.testing().path(AMP).path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(NOVOSTROJKI).should(hasHref(equalTo(urlSteps.testing().path(AMP)
                .path("/sankt-peterburg_i_leningradskaya_oblast/kupit/novostrojka/").toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Ссылка на новостройки в Магадане если нет новостроек")
    public void shouldSeeNovostrojkiUrlMagadanAmp() {
        urlSteps.testing().path(AMP).path(MAGADAN).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(NOVOSTROJKI).should(hasHref(equalTo(urlSteps.testing()
                .uri("/amp/magadan/kupit/kvartira/novostroyki/").toString())));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меню. Ссылка на новостройки в Омске")
    public void shouldSeeNovostrojkiUrlOmskAmp() {
        urlSteps.testing().path(AMP).path(OMSK).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().ampMenu().link(NOVOSTROJKI).should(hasHref(equalTo(urlSteps.testing().path(AMP)
                .path("/omsk/kupit/novostrojka/").toString())));
    }

}
