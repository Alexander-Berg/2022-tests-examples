package ru.auto.tests.amp.sidebar;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.FINANCE;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар - ссылки")
@Feature(AutoruFeatures.AMP)
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SidebarUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String urlTitle;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Мои объявления", "https://%s/my/all/"},
                {"Избранное", "https://%s/like/"},
                {"Сохраненные поиски", "https://%s/like/searches/"},
                {"Мои отзывы", "https://%s/my/reviews/"},
                {"Заявки на кредит", "https://%s/promo" + FINANCE},
                {"Гараж", "https://%s/garage/"},
                {"ПроАвто", "https://%s/amp/history/"},
                {"Добавить объявление", "https://%s/promo/from-web-to-app/"},

                {"Легковые", "https://%s/moskva/amp/cars/all/"},
                {"Коммерческие", "https://%s/moskva/amp/lcv/all/"},
                {"Мото", "https://%s/moskva/amp/motorcycle/all/"},
                {"Каталог", "https://%s/amp/catalog/cars/"},
                {"Отзывы", "https://%s/reviews/"},
                {"Журнал", "https://mag.%s/"},
                {"Учебник", "https://mag.%s/theme/uchebnik/?from=autoru_curtain"},
                {"Оценить авто", "https://%s/cars/evaluation/"},
                {"Купить ОСАГО", "https://%s/promo/osago/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "mobile/SearchCarsAll").post();

        basePageSteps.setWindowMaxHeight();
        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(ALL).open();
        basePageSteps.onListingPage().header().sidebarButton().should(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        basePageSteps.onListingPage().sidebar().button(urlTitle).click();
        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain())).ignoreParam("_gl").shouldNotSeeDiff();
    }
}
