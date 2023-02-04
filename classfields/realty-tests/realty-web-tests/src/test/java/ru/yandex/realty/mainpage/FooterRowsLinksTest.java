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

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Ссылки в подвале")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FooterRowsLinksTest {

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
                {"Авиамоторная", "/moskva_i_moskovskaya_oblast/kupit/kvartira/metro-aviamotornaya/"},
                {"Академический", "/moskva_i_moskovskaya_oblast/kupit/kvartira/dist-akademicheskij-12446/"},
                {"Санкт-Петербург", "/sankt-peterburg/kupit/kvartira/"},
                {"Студии", "/moskva/kupit/kvartira/studiya/"},
                {"Дома", "/moskva/kupit/dom/"},
                {"Улицы", "/moskva/streets/"},
                {"Районы", "/moskva/districts/"},
                {"Станции метро", "/moskva/metro-stations/"},
                {"Станции пригородных поездов", "/moskva/railways/"},
                {"Снять", "/moskva/snyat/kvartira/"},
        });
    }

    @Before
    public void openMainPage() {
        urlSteps.setMoscowCookie();
        urlSteps.testing().open();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeRowLinks() {
        basePageSteps.onBasePage().footer().link(text)
                .should(hasAttribute("href", containsString(expected)));
    }
}
