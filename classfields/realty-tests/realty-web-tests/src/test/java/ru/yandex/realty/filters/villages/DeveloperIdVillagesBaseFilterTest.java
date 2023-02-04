package ru.yandex.realty.filters.villages;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Фильтр поиска по коттеджным поселкам.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DeveloperIdVillagesBaseFilterTest {

    private static final String DEVELOPER = "Застройщик";
    private static final String TEST_DEVELOPER = "Теорема";
    private static final String DEVELOPER_NAME = "developerName";
    private static final String DEVELOPER_ID = "developerId";
    private static final String VALUE = "42295";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбираем «Застройщика» ")
    public void shouldSeeDeveloperId() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onVillageListing().openExtFilter();
        basePageSteps.onVillageListing().extendFilters().input(DEVELOPER, TEST_DEVELOPER);
        basePageSteps.onVillageListing().extendFilters().suggest().get(0).click();
        basePageSteps.onVillageListing().extendFilters().applyFiltersButton().click();
        urlSteps.path("/z-teorema-42295/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Выбран «Застройщика» при переходе по урлу")
    public void shouldSeeDeveloperIdChecked() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(DEVELOPER_ID, VALUE)
                .open();
        basePageSteps.onVillageListing().openExtFilter();
        basePageSteps.onVillageListing().extendFilters().input(DEVELOPER).should(hasValue(TEST_DEVELOPER));
    }
}
