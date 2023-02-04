package ru.auto.tests.mobile.sidebar;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.FINANCE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар - ссылки")
@Feature(SIDEBAR)
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SidebarUnauthUrlsTest {

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

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Мои объявления", "https://%s/my/all/"},
                {"Избранное", "https://%s/like/"},
                {"Сохранённые поиски", "https://%s/like/searches/"},
                {"Сообщения", "https://%1$s/chat-auth/?r2=https://%1$s/"},
                {"Промокоды", "https://auth.%1$s/login/?r=https://%1$s/my/promo-codes/"},
                {"Мои отзывы", "https://%s/my/reviews/"},
                {"Заявки на кредит", "https://%s/promo" + FINANCE},
                {"ПроАвто", "https://%s/history/"},
                {"Безопасная сделка", "https://%s/promo/safe-deal/"},
                {"Добавить объявление", "https://%s/promo/from-web-to-app/"},

                {"Легковые", "https://%s/moskva/"},
                {"Коммерческие", "https://%s/moskva/lcv/all/"},
                {"Мото", "https://%s/moskva/motorcycle/all/"},
                {"Дилеры", "https://%s/dilery/cars/new/"},
                {"Каталог", "https://%s/catalog/cars/"},
                {"Гараж", "https://%s/garage/"},
                {"Оценить авто", "https://%s/cars/evaluation/"},
                {"Отзывы", "https://%s/reviews/"},
                {"Журнал", "https://mag.%s/?utm_campaign=main_menu&utm_content=main_menu&utm_medium=cpm&utm_source=auto-ru"},
                {"Учебник", "https://mag.%s/theme/uchebnik/?from=autoru_curtain&utm_campaign=main_menu_uchebnik&utm_content=main_menu_uchebnik&utm_medium=cpm&utm_source=auto-ru"},
                {"Купить ОСАГО", "https://%s/promo/osago/"},
                {"Саша Котов", "https://%s/kot/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().open();
        basePageSteps.onMainPage().header().sidebarButton().should(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").update();

        basePageSteps.onMainPage().sidebar().button(urlTitle).hover().click();
        waitSomething(2, TimeUnit.SECONDS);
        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }
}
