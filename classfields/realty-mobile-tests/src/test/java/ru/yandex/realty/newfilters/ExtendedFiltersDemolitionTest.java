package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;

@DisplayName("Расширенные фильтры. Реновация")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ExtendedFiltersDemolitionTest {

    private static final String RENOVATION = "Реновация";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.onMobileMainPage().extendFilters().button("Вторичка").click();
        basePageSteps.scrollToElement(
                basePageSteps.onMobileMainPage().extendFilters().byName(RENOVATION));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Показать дома под снос»")
    public void shouldSeeDemolitionBuildings() {
        basePageSteps.onMobileMainPage().extendFilters().byName(RENOVATION).button("Показать дома под снос").click();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path("/vtorichniy-rynok-i-pod-snos/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Не показывать дома под снос»")
    public void shouldNNotSeeDemolitionBuildings() {
        basePageSteps.onMobileMainPage().extendFilters().byName(RENOVATION)
                .button("Не показывать дома под снос").click();
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("newFlat", "NO").queryParam("expectDemolition", "NO")
                .shouldNotDiffWithWebDriverUrl();
    }
}
