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
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;

@DisplayName("Главная. Ссылки в футере")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FooterLinksTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps user;

    @Parameterized.Parameter
    public String text;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{index} -ссылка {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Вся недвижимость", "/"},
                {"Реклама", "https://yandex.ru/adv/products/display/realty"},
                {"Пользовательское соглашение", "https://yandex.ru/legal/realty_termsofuse/"},
                {"Помощь", "https://yandex.ru/support/realty/"},
                {"Яндекс.Вертикали", "https://yandex.ru"},
        });
    }

    @Before
    public void openMainPage() {
        urlSteps.testing().open();
    }

    @Test
    @Owner(VICDEV)
    public void shouldSeeLinks() {
        user.onBasePage().footer().link(text)
                .should(hasAttribute("href", containsString(expected)));
    }
}
