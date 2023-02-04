package ru.auto.tests.poffer.footer;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FOOTER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_ADVERTISMENT;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Футер - ссылки")
@Feature(FOOTER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FooterUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String urlTitle;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Разместить рекламу", YANDEX_ADVERTISMENT},
                {"Дилерам", "https://auto.ru/dealer/"},
                {"Помощь", YANDEX_SUPPORT_AUTORU},
                {"О проекте", "https://auto.ru/home/"},
                {"Аналитика Авто.ру", "https://mag.auto.ru/tag/research/?from=morda-stat"},
                {"Москва и Московская область", "https://%s/moskovskaya_oblast/"},
                {"Пользовательское соглашение", YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("poffer/UserDraftCarsEmpty")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        basePageSteps.onMainPage().footer().button(urlTitle).waitUntil(isDisplayed()).click();

        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }
}
