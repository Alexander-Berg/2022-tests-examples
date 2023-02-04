package ru.auto.tests.forms.header;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.MOTO;

@DisplayName("Шапка - легковые")
@Feature(HEADER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeaderUrlsCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Название ссылки")
    @Parameterized.Parameter
    public String title;

    //@Parameter("Ссылка")
    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"С пробегом", "https://%s/moskva/cars/used/"},
                {"Новые", "https://%s/moskva/cars/new/"},
                {"Каталог", "https://%s/catalog/cars/"},
                {"Дилеры", "https://%s/moskva/dilery/cars/all/"},
                {"Отзывы", "https://%s/reviews/"},
                {"Видео", "https://%s/video/"},
                {"Новинки автопрома", "https://%s/moskva/cars/all/tag/new4new/"},
                {"Внедорожник", "https://%s/moskva/cars/all/body-allroad/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOTO).path(ADD).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылкам в меню «Легковые»")
    public void shouldClickCarsMenuUrls() {
        basePageSteps.onFormsPage().header().line2().button("Легковые").hover();
        basePageSteps.onFormsPage().header().line2().dropdown().button(title).click();
        urlSteps.fromUri(format(url, urlSteps.getConfig().getTestingURI().getHost())).shouldNotSeeDiff();
    }
}