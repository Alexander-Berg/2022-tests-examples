package ru.auto.tests.mobilereviews.sidebar;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SIDEBAR;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.FINANCE;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Сайдбар - ссылки")
@Feature(SIDEBAR)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SidebarUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

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
                {"Сохранённые поиски", "https://%s/like/searches/"},
                {"Мои отзывы", "https://%s/my/reviews/"},
                {"Заявки на кредит", "https://%s/promo" + FINANCE},

                {"Гараж", "https://%s/garage/"},
                {"ПроАвто", "https://%s/history/"},
                {"Добавить объявление", "https://%s/promo/from-web-to-app/"},

                {"Легковые", "https://%s/moskva/"},
                {"Коммерческие", "https://%s/moskva/lcv/all/"},
                {"Мото", "https://%s/moskva/motorcycle/all/"},
                {"Дилеры", "https://%s/dilery/cars/new/"},
                {"Каталог", "https://%s/catalog/cars/"},
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
        urlSteps.testing().path(REVIEWS).open();
        basePageSteps.onMainPage().header().sidebarButton().click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        basePageSteps.onReviewsMainPage().sidebar().button(urlTitle).should(isDisplayed()).click();
        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain())).ignoreParam("geo_id")
                .ignoreParam("rgid").shouldNotSeeDiff();
    }
}
