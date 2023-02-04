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

@DisplayName("Главная. Ссылки. Поделяшки")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FooterShareLinksTest {

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
                {"dzen", "https://zen.yandex.ru/realty.yandex.ru?clid=300&country_code=ru&_csrf=" +
                        "01bc44b41c7f56365604936ef7600ddbe24cdbf2-1550751328892&token=&from_page=other_page"},
                {"vk", "https://vk.com/yandex.realty?_smt=feed%3A2"},
        });
    }

    @Before
    public void openMainPage() {
        urlSteps.testing().open();
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeShareLinks() {
        basePageSteps.onBasePage().footer().socialNetLink(text).should(hasHref(equalTo(expected)));
    }
}
