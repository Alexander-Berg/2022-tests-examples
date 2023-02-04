package ru.auto.tests.desktopreviews.header;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

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

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/reviews/", "Объявления", "/moskva/cars/all/"},
                {"/reviews/", "Дилеры", "/moskva/dilery/cars/all/"},
                {"/reviews/", "Каталог", "/catalog/cars/"},
                {"/reviews/", "Отзывы", "/reviews/"},
                {"/reviews/", "Видео", "/video/"},

                {"/reviews/cars/audi/a3/20785010/?sort=updateDate-desc",
                        "Объявления", "/moskva/cars/audi/a3/20785010/all/"},
                {"/reviews/cars/audi/a3/20785010/?sort=updateDate-desc",
                        "Дилеры", "/moskva/dilery/cars/all/"},
                {"/reviews/cars/audi/a3/20785010/?sort=updateDate-desc",
                        "Каталог", "/catalog/cars/audi/a3/20785010/"},

                {"/reviews/moto/", "Объявления", "/moskva/motorcycle/all/?parent_category=moto"},

                {"/reviews/trucks/", "Объявления", "/moskva/lcv/all/?parent_category=trucks"},

                {"/reviews/cars/all/?sort=updateDate-desc&catalog_filter=mark%3DAUDI%2Cmodel%3DA3%2Cgeneration%3D20785010&catalog_filter=mark%3DAUDI%2Cmodel%3DA3%2Cgeneration%3D7979586",
                        "Объявления", "/moskva/cars/all/?catalog_filter=mark=AUDI,model=A3,generation=20785010&catalog_filter=mark=AUDI,model=A3,generation=7979586"},
                {"/reviews/cars/all/?sort=updateDate-desc&catalog_filter=mark%3DAUDI%2Cmodel%3DA3%2Cgeneration%3D20785010&catalog_filter=mark%3DAUDI%2Cmodel%3DA3%2Cgeneration%3D7979586",
                        "Дилеры", "/moskva/dilery/cars/all/"},
                {"/reviews/cars/all/?catalog_filter=mark%3DAUDI%2Cmodel%2CA3generation%3D20785010&catalog_filter=mark%3DAUDI%2Cmodel%3DA3%2Cgeneration%3D7979586&sort=updateDate-desc",
                        "Каталог", "/catalog/cars/audi/a3/7979586/"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке в подшапке")
    public void shouldClickSubheaderUrl() {
        basePageSteps.setWideWindowSize(1024);

        urlSteps.testing().pathsAndParams(startUrl).open();
        basePageSteps.onReviewsMainPage().subHeader().button(subHeaderUrlTitle).click();

        urlSteps.testing().pathsAndParams(subHeaderUrl).shouldNotSeeDiff();
    }
}
