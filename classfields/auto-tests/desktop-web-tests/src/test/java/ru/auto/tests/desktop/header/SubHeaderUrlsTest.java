package ru.auto.tests.desktop.header;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;

@DisplayName("Подшапка - ссылки")
@Feature(HEADER)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SubHeaderUrlsTest {

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
    public String startUrl;

    @Parameterized.Parameter(1)
    public String urlTitle;

    @Parameterized.Parameter(2)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"https://%s/moskva", "Объявления", "https://%s/moskva/cars/all/"},
                {"https://%s/moskva", "Дилеры", "https://%s/moskva/dilery/cars/all/"},
                {"https://%s/moskva", "Каталог", "https://%s/catalog/cars/"},
                {"https://%s/moskva", "Отзывы", "https://%s/reviews/"},
                {"https://%s/moskva", "Видео", "https://%s/video/"},

                {"https://%s/moskva/cars/all/", "Дилеры", "https://%s/moskva/dilery/cars/all/"},
                {"https://%s/moskva/cars/all/", "Каталог", "https://%s/catalog/cars/"},
                {"https://%s/moskva/cars/all/", "Отзывы", "https://%s/reviews/"},
                {"https://%s/moskva/cars/all/", "Видео", "https://%s/video/"},

                {"https://%s/moskva/cars/toyota/all/", "Дилеры", "https://%s/moskva/dilery/cars/all/"},
                {"https://%s/moskva/cars/toyota/all/", "Каталог", "https://%s/catalog/cars/toyota/"},
                {"https://%s/moskva/cars/toyota/all/", "Отзывы", "https://%s/reviews/cars/toyota/"},
                {"https://%s/moskva/cars/toyota/all/", "Видео", "https://%s/video/cars/toyota/"},
                {"https://%s/moskva/cars/toyota/all/", "Статистика цен", "https://%s/stats/cars/toyota/?section=all"},

                {"https://%s/moskva/motorcycle/all/", "Отзывы", "https://%s/reviews/moto/motorcycle/"},
                {"https://%s/moskva/motorcycle/harley_davidson/all/", "Отзывы", "https://%s/reviews/moto/motorcycle/harley_davidson/"},
                {"https://%s/moskva/motorcycle/harley_davidson/dyna_super_glide/all/", "Отзывы", "https://%s/reviews/moto/motorcycle/harley_davidson/dyna_super_glide/"},

                {"https://%s/moskva/truck/all/", "Отзывы", "https://%s/reviews/trucks/truck/"},
                {"https://%s/moskva/truck/zil/all/", "Отзывы", "https://%s/reviews/trucks/truck/zil/"},
                {"https://%s/moskva/truck/zil/5301/all/", "Отзывы", "https://%s/reviews/trucks/truck/zil/5301/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsBreadcrumbsToyota",
                "desktop/SearchMotoBreadcrumbs",
                "desktop/SearchMotoBreadcrumbsMark",
                "desktop/SearchMotoBreadcrumbsMarkModel",
                "desktop/SearchMotoAll",
                "desktop/SearchMotoMark",
                "desktop/SearchMotoMarkModel",
                "desktop/SearchTrucksBreadcrumbs",
                "desktop/SearchTrucksBreadcrumbsMark",
                "desktop/SearchTrucksBreadcrumbsMarkModel",
                "desktop/SearchTrucksAll",
                "desktop/SearchTrucksMark",
                "desktop/SearchTrucksMarkModel",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.fromUri(format(startUrl, urlSteps.getConfig().getBaseDomain())).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылкам в подшапке")
    public void shouldClickSubHeaderUrls() {
        basePageSteps.onBasePage().subHeader().button(urlTitle).click();
        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }
}