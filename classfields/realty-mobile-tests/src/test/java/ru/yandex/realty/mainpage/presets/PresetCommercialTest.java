package ru.yandex.realty.mainpage.presets;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.ROSSIYA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.main.PresetsSection.COMMERCIAL;
import static ru.yandex.realty.step.UrlSteps.REDIRECT_FROM_RGID;
import static ru.yandex.realty.step.UrlSteps.TRUE_VALUE;

@Issue("VERTISTEST-1352")
@Feature(MAIN)
@DisplayName("Ссылки пресета «Коммерческая недвижимость»")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PresetCommercialTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "Ссылка «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Купить офис", "/rossiya/kupit/kommercheskaya-nedvizhimost/ofis/"},
                {"Купить помещение", "/rossiya/kupit/kommercheskaya-nedvizhimost/pomeshchenie-svobodnogo-naznacheniya/"},
                {"Аренда офиса", "/rossiya/snyat/kommercheskaya-nedvizhimost/ofis/"},
                {"Аренда помещения", "/rossiya/snyat/kommercheskaya-nedvizhimost/pomeshchenie-svobodnogo-naznacheniya/"},
                {"Аренда торговой площади", "/rossiya/snyat/kommercheskaya-nedvizhimost/torgovoe-pomeshchenie/"},
                {"Аренда склада", "/rossiya/snyat/kommercheskaya-nedvizhimost/skladskoe-pomeshchenie/"}
        });
    }

    @Before
    public void openMainPage() {
        urlSteps.testing().path(ROSSIYA).queryParam(REDIRECT_FROM_RGID, TRUE_VALUE).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.scrollUntilExists(() -> basePageSteps.onMobileMainPage().preset(COMMERCIAL));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки пресета «Коммерческая недвижимость»")
    public void shouldSeePresetBuyFlatLinks() {
        basePageSteps.onMobileMainPage().preset(COMMERCIAL).link(title).should(hasHref(equalTo(
                urlSteps.testing().uri(path).toString())));
    }
}
