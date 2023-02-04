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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Ссылки в подвале. Скирншоты")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FooterRowsScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public String text;

    @Parameterized.Parameters(name = "{index} -ссылка {0}")
    public static Collection<String> testParameters() {
        return asList(
                "Все станции метро",
                "Все районы",
                "Все города"
        );
    }

    @Before
    public void openMainPage() {
        urlSteps.setMoscowCookie();
        compareSteps.resize(1920, 7000);
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeScreenshot() {
        urlSteps.testing().open();
        basePageSteps.scrollToElement(basePageSteps.onBasePage().footer());
        basePageSteps.onBasePage().footer().spanLink(text).click();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().footer().content());

        urlSteps.setProductionHost().open();
        basePageSteps.scrollToElement(basePageSteps.onBasePage().footer());
        basePageSteps.onBasePage().footer().spanLink(text).click();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().footer().content());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
