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
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.mobile.element.main.PresetsSection.RENT_FLAT;

@Issue("VERTISTEST-1352")
@Feature(MAIN)
@DisplayName("Ссылки пресета «Снять квартиру»")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PresetRentFlatTest {

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
                {"На длительный срок", "/moskva/snyat/kvartira/"},
                {"Студии", "/moskva/snyat/kvartira/studiya/"},
                {"1-комнатные", "/moskva/snyat/kvartira/odnokomnatnaya/"},
                {"2-комнатные", "/moskva/snyat/kvartira/dvuhkomnatnaya/"},
                {"Без посредников", "/moskva/snyat/kvartira/bez-posrednikov/"},
                {"Комнаты на длительный срок", "/moskva/snyat/komnata/"}
        });
    }

    @Before
    public void openMainPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.scrollUntilExists(() -> basePageSteps.onMobileMainPage().preset(RENT_FLAT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки пресета «Снять квартиру»")
    public void shouldSeePresetBuyFlatLinks() {
        basePageSteps.onMobileMainPage().preset(RENT_FLAT).link(title).should(hasHref(equalTo(
                urlSteps.testing().uri(path).toString())));
    }

}
