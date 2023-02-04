package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Ссылки «Выберите лучший район для жизни»")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeatMapLinkTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String link;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public String propertyValue;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Цена аренды квартиры", SNYAT, "price-rent"},
                {"Инфраструктура", KUPIT, "infrastructure"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Smoke.class, Production.class})
    @DisplayName("Переходим по ссылке, видим урл с соответствующим слоем на карте")
    public void shouldSeeLink() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().heatMapsBlock().link(link).waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(path).path(KVARTIRA).path(KARTA)
                .queryParam("layer", propertyValue).ignoreMapCoordinate().shouldNotDiffWithWebDriverUrl();
    }
}
