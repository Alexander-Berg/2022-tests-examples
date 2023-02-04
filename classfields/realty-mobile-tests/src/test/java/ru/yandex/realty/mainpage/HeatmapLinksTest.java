package ru.yandex.realty.mainpage;

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
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@Issue("VERTISTEST-1352")
@Feature(MAIN)
@DisplayName("Ссылки тепловых карт")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeatmapLinksTest {

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

    @Parameterized.Parameter(2)
    public String paramValue;

    @Parameterized.Parameters(name = "Ссылка «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Инфраструктура", "/kupit/", "infrastructure"},
                {"Цена аренды квартиры", "/snyat/", "price-rent"},
                {"Цена продажи квартиры", "/kupit/", "price-sell"},
                {"Прогноз окупаемости", "/kupit/", "profitability"},
                {"Транспортная доступность", "/kupit/", "transport"},
                {"Школы и их рейтинг", "/kupit/", "education"}
        });
    }

    @Before
    public void openMainPage() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.scrollUntilExists(() -> basePageSteps.onMobileMainPage().heatmap());

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылки тепловых карт")
    public void shouldSeeHeatmapLinks() {
        basePageSteps.onMobileMainPage().heatmap().link(title).should(hasHref(equalTo(
                urlSteps.testing().path(MOSKVA).path(path).path("/kvartira/karta/")
                        .queryParam("layer", paramValue).toString())));
    }

}
