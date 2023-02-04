package ru.auto.tests.mobilereviews.header;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;

@DisplayName("Подшапка - ссылки")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SubHeaderUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String subHeaderUrlTitle;

    @Parameterized.Parameter(2)
    public String subHeaderUrl;

    @Parameterized.Parameters(name = "{0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"https://%s/reviews/", "Объявления", "https://%s/moskva/cars/all/?from=top_menu_secondline"},
                {"https://%s/reviews/", "Каталог", "https://%s/catalog/cars/?from=top_menu_secondline"},
                {"https://%s/reviews/", "Гараж", "https://%s/garage/?from=top_menu_secondline"},
                {"https://%s/reviews/", "Отзывы", "https://%s/reviews/?from=top_menu_secondline"},
                {"https://%s/reviews/", "Видео", "https://%s/video/?from=top_menu_secondline"},

                {"https://%s/reviews/moto/", "Объявления", "https://%s/moskva/motorcycle/all/?from=top_menu_secondline"},
                {"https://%s/reviews/moto/", "Отзывы", "https://%s/reviews/moto/?from=top_menu_secondline"},

                {"https://%s/reviews/trucks/", "Объявления", "https://%s/moskva/lcv/all/?from=top_menu_secondline"},
                {"https://%s/reviews/trucks/", "Отзывы", "https://%s/reviews/trucks/?from=top_menu_secondline"},

                {"https://%s/reviews/cars/audi/a3/20785010/?sort=updateDate-desc",
                        "Объявления", "https://%s/moskva/cars/audi/a3/all/?from=top_menu_secondline"},
                {"https://%s/reviews/cars/audi/a3/20785010/?sort=updateDate-desc",
                        "Видео", "https://%s/video/cars/audi/a3/?from=top_menu_secondline"},
                {"https://%s/reviews/cars/audi/a3/20785010/?sort=updateDate-desc",
                        "Каталог", "https://%s/catalog/cars/audi/a3/?from=top_menu_secondline"},
                {"https://%s/reviews/cars/audi/a3/20785010/?sort=updateDate-desc",
                        "Статистика цен", "https://%s/stats/cars/audi/a3/?parent_category=cars&from=top_menu_secondline"},
                {"https://%s/reviews/cars/audi/a3/20785010/?sort=updateDate-desc",
                        "Гараж", "https://%s/garage/?from=top_menu_secondline"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке в подшапке")
    @Category({Regression.class})
    public void shouldClickSubheaderUrl() {
        urlSteps.fromUri(format(startUrl, urlSteps.getConfig().getBaseDomain())).open();

        basePageSteps.onReviewsMainPage().subHeader().url(subHeaderUrlTitle).click();
        urlSteps.fromUri(format(subHeaderUrl, urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }
}
