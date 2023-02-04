package ru.yandex.realty.mappage;

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
import org.junit.runners.Parameterized;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAP;

@DisplayName("Карта. Реклама на большом экране")
@Feature(MAP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class MapCornerAdTest {

    private static final String NOT_NULL_DESCRIPTION = "Площадь элемента не нулевая";

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
    public String region;

    @Parameterized.Parameters(name = "Проверяем что есть ненулевой баннер рекламы")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Татарстан", "/tatarstan/"},
                {"Москва", "/moskva/"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим рекламу внизу в углу на большом экране. Татарстан")
    public void shouldSeeCornerAdTatarstan() {
        urlSteps.testing().path("/tatarstan/").path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        int height = basePageSteps.onMapPage().mapAdCorner().getRect().getHeight();
        int width = basePageSteps.onMapPage().mapAdCorner().getRect().getWidth();
        assertThat(height * width).describedAs(NOT_NULL_DESCRIPTION).isNotEqualTo(0);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим рекламу внизу в углу на большом экране. Москва")
    public void shouldSeeCornerAdMoscow() {
        urlSteps.testing().path("/moskva/").path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        int height = basePageSteps.onMapPage().mapAdCorner().getRect().getHeight();
        int width = basePageSteps.onMapPage().mapAdCorner().getRect().getWidth();
        assertThat(height * width).describedAs(NOT_NULL_DESCRIPTION).isNotEqualTo(0);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим рекламу внизу в углу на большом экране. В краснодаре баннер нулевой")
    public void shouldSeeCornerAdKrasnodar() {
        urlSteps.testing().path("/krasnodarskiy_kray/").path(KUPIT).path(NOVOSTROJKA).path(KARTA).open();
        int height = basePageSteps.onMapPage().mapAdCorner().getRect().getHeight();
        int width = basePageSteps.onMapPage().mapAdCorner().getRect().getWidth();
        assertThat(height * width).describedAs("Площадь элемента нулевая").isEqualTo(0);
    }
}
