package ru.yandex.realty.filters.villages;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Фильтр поиска по коттеджным поселкам.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ChpuVillagesTest {

    public static final String TEST_DIRECTION = "Горьковское шоссе";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onVillageListing().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("2 параметра КП + бизнесс класс + сдан")
    public void shouldSee2ParamsVillagesInUrl() {
        basePageSteps.onVillageListing().extendFilters().select("Класс посёлка", "Бизнес");
        basePageSteps.onVillageListing().extendFilters().select("Срок сдачи", "Сдан");
        basePageSteps.onVillageListing().extendFilters().applyFiltersButton().click();
        urlSteps.path("/s-kluchami-i-kp-biznes/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Шоссе + 2 параметра: КП + горьковское шоссе + канализация + газ")
    public void shouldSee2ParamsWithDirectionVillagesInUrl() {
        basePageSteps.onVillageListing().extendFilters().input("Адрес, посёлок", TEST_DIRECTION);
        basePageSteps.onVillageListing().extendFilters().suggest(TEST_DIRECTION).click();
        basePageSteps.onVillageListing().extendFilters().button("Коммуникации").click();
        basePageSteps.onVillageListing().extendFilters().selectPopup().checkBox("Газ").click();
        basePageSteps.onVillageListing().extendFilters().selectPopup().checkBox("Канализация").click();
        basePageSteps.onVillageListing().extendFilters().applyFiltersButton().click();
        urlSteps.path("/shosse-gorkovskoe/").path("/s-gazom-i-s-kanalizaciej/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Не формируем ЧПУ КП + парк + сдан + ИЖС")
    public void shouldSee3ParamsVillagesInUrl() {
        basePageSteps.onVillageListing().extendFilters().select("Срок сдачи", "Сдан");
        basePageSteps.onVillageListing().extendFilters().selectCheckBox("Рядом парк");
        basePageSteps.onVillageListing().extendFilters().selectCheckBox("ИЖС");
        basePageSteps.onVillageListing().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("infrastructureType", "PARK").queryParam("deliveryDate", "FINISHED")
                .queryParam("landType", "IZHS").shouldNotDiffWithWebDriverUrl();
    }
}
