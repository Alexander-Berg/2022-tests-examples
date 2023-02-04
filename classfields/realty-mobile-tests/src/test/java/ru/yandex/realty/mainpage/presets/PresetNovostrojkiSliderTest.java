package ru.yandex.realty.mainpage.presets;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.main.PresetsSection.NOVOSTROJKI;

@Issue("VERTISTEST-1352")
@Feature(MAIN)
@DisplayName("Ссылки в айтемах слайдера пресета Новостроек")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class PresetNovostrojkiSliderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openMainPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.scrollUntilExists(() -> basePageSteps.onMobileMainPage().preset(NOVOSTROJKI));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Видим что ссылки офферов в карусели ностороек ведут на новостроечный листинг")
    public void shouldSeeSliderItemsUrl() {
        basePageSteps.onMobileMainPage().preset(NOVOSTROJKI).sliderItems().forEach(item ->
                item.link().should(hasHref(containsString("/kupit/novostrojka/"))));
    }
}
