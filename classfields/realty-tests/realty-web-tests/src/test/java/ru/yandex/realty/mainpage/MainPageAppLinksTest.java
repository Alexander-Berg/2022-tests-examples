package ru.yandex.realty.mainpage;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;

@DisplayName("Главная. Ссылки. Приложения")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MainPageAppLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String text;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{index} -ссылка {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Яндекс Недвижимость для Android", "https://redirect.appmetrica.yandex.com/serve/744703322706954699"},
                {"Яндекс Недвижимость для iOS", "https://redirect.appmetrica.yandex.com/serve/888818511672008835"},
                {"Яндекс Недвижимость для Huawei", "https://appgallery8.huawei.com/#/app/C101229283"}
        });
    }

    @Before
    public void openMainPage() {
        urlSteps.testing().open();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeShareLinks() {
        basePageSteps.onMainPage().img(text).should(hasHref(equalTo(expected)));
    }
}
