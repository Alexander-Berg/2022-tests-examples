package ru.yandex.realty.menu;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MAGADAN;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MENU;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Issue("VERTISTEST-1352")
@Feature(MENU)
@DisplayName("Регионально зависимые ссылки в меню")
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
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. «Выбери район для жизни» нет в Магадане")
    public void shouldNotSeeChoiseAreaForLiveMagadan() {
        urlSteps.testing().path(MAGADAN).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(CHOOSE_AREA_FOR_LIVE).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Ссылка «Выбери район для жизни» в Санкт-Петербурге")
    public void shouldSeeChoiceAreaForLiveSanktPeterburg() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(CHOOSE_AREA_FOR_LIVE).should(hasHref(equalTo(urlSteps.testing()
                .uri("/sankt-peterburg/kupit/kvartira/karta/?isHeatmapsExpanded=true").toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Ссылка «Выбери район для жизни» в Москве")
    public void shouldSeeChoiceAreaForLiveMoskva() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(CHOOSE_AREA_FOR_LIVE).should(hasHref(equalTo(urlSteps.testing()
                .uri("/moskva/kupit/kvartira/karta/?isHeatmapsExpanded=true").toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Коттеджных поселков нет в Магадане")
    public void shouldNotSeeVillagesMagadan() {
        urlSteps.testing().path(MAGADAN).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(KOTTEDZHNYE_POSELKI).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Ссылка на коттеджные поселки в Санкт-Петербурге")
    public void shouldSeeVillagesSanktPeterburg() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(KOTTEDZHNYE_POSELKI).should(hasHref(equalTo(urlSteps.testing()
                .uri("/sankt-peterburg_i_leningradskaya_oblast/kupit/kottedzhnye-poselki/").toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Ссылка на коттеджные поселки в Москве")
    public void shouldSeeVillagesMoskva() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(KOTTEDZHNYE_POSELKI).should(hasHref(equalTo(urlSteps.testing()
                .uri("/moskva_i_moskovskaya_oblast/kupit/kottedzhnye-poselki/").toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Квартир от ПИК нет в Магадане")
    public void shouldNotSeePikMagadan() {
        urlSteps.testing().path(MAGADAN).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(KVARTIRI_PIK).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Квартир от ПИК нет в СПБ")
    public void shouldNotSeePikSanktPeterburg() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(KVARTIRI_PIK).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Ссылка на новостройки в Москве")
    public void shouldSeeNovostrojkiUrlMoskva() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(NOVOSTROJKI).should(hasHref(equalTo(urlSteps.testing()
                .uri("/moskva_i_moskovskaya_oblast/kupit/novostrojka/").toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Ссылка на новостройки в Санкт-Петербурге")
    public void shouldSeeNovostrojkiUrlSanktPeterburg() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(NOVOSTROJKI).should(hasHref(equalTo(urlSteps.testing()
                .uri("/sankt-peterburg_i_leningradskaya_oblast/kupit/novostrojka/").toString())));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Меню. Ссылка на новостройки в Магадане")
    public void shouldSeeNovostrojkiUrlMagadan() {
        urlSteps.testing().path(MAGADAN).open();
        basePageSteps.onBasePage().menuButton().click();

        basePageSteps.onBasePage().menu().link(NOVOSTROJKI).should(hasHref(equalTo(urlSteps.testing()
                .uri("/magadan/kupit/kvartira/novostroyki/").toString())));
    }

}
