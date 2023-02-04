package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Сслыки")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MainPageLinksWithPropsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String link;

    @Parameterized.Parameter(2)
    public String path;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameters(name = "{0} -> {1}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Загородная недвижимость", "Купить дом", "/kupit/dom/"},
                {"Загородная недвижимость", "Снять дом надолго", "/snyat/dom/"},
                {"Загородная недвижимость", "Купить таунхаус", "/kupit/dom/townhouse/"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем урл при переходе по ссылке")
    public void shouldSeeTypeInUrl() {
        urlSteps.testing().path(SPB_I_LO).open();
        basePageSteps.onMainPage().mainBlock(name).link(link).waitUntil(isDisplayed()).click();
        urlSteps.path(path).shouldNotDiffWithWebDriverUrl();
    }
}
