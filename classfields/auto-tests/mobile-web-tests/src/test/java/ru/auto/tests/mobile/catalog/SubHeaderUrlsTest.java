package ru.auto.tests.mobile.catalog;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;

@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Каталог - ссылки в подшапке")
@Feature(AutoruFeatures.CATALOG)
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
    public String tabTitle;

    @Parameterized.Parameter(2)
    public String tabUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"", "Объявления", "/moskva/cars/all/"},
                {"", "Каталог", "/catalog/cars/"},
                {"", "Видео", "/video/"},
                {"/marks/", "Объявления", "/moskva/cars/all/"},
                {"/marks/", "Видео", "/video/"},
                {"/vaz/", "Объявления", "/moskva/cars/vaz/all/"},
                {"/vaz/", "Каталог", "/catalog/cars/vaz/"},
                {"/vaz/", "Видео", "/video/cars/vaz/"},
                {"/vaz/", "Статистика цен", "/stats/cars/vaz/"},
                {"/vaz/vesta/", "Объявления", "/moskva/cars/vaz/vesta/all/"},
                {"/vaz/granta/", "Каталог", "/catalog/cars/vaz/granta/"},
                {"/vaz/vesta/", "Видео", "/video/cars/vaz/vesta/"},
                {"/vaz/vesta/", "Статистика цен", "/stats/cars/vaz/vesta/"},
                {"/vaz/vesta/20417749/", "Объявления", "/moskva/cars/vaz/vesta/20417749/all/"},
                {"/vaz/vesta/20417749/", "Каталог", "/catalog/cars/vaz/vesta/20417749/"},
                {"/vaz/vesta/20417749/", "Видео", "/video/cars/vaz/vesta/20417749/"},
                {"/vaz/vesta/20417749/", "Статистика цен", "/stats/cars/vaz/vesta/20417749/"},
                {"/vaz/vesta/20417749/20417777/", "Объявления", "/moskva/cars/vaz/vesta/20417749/20417777/all/"},
                {"/vaz/vesta/20417749/20417777/", "Каталог", "/catalog/cars/vaz/vesta/20417749/20417777/"},
                {"/vaz/vesta/20417749/20417777/", "Видео", "/video/cars/vaz/vesta/20417749/?configuration_id=20417777"},
                {"/vaz/vesta/20417749/20417777/", "Статистика цен", "/stats/cars/vaz/vesta/20417749/20417777/"},
                {"/vaz/vesta/20417749/20417777/specifications/20417777_20726112_20417814/", "Объявления",
                        "/moskva/cars/vaz/vesta/20417749/20417777/20417814/all/"},
                {"/vaz/vesta/20417749/20417777/specifications/20417777_20726112_20417814/", "Каталог",
                        "/catalog/cars/vaz/vesta/20417749/20417777/specifications/20417777_20726112_20417814/"},
                {"/vaz/vesta/20417749/20417777/specifications/20417777_20726112_20417814/", "Видео",
                        "/video/cars/vaz/vesta/20417749/?configuration_id=20417777&complectation_id=20417777_20726112_20417814"},
                {"/vaz/vesta/20417749/20417777/specifications/20417777_20726112_20417814/", "Статистика цен",
                        "/stats/cars/vaz/vesta/20417749/20417777/20417777_20726112_20417814/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(startUrl).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке в подшапке")
    public void shouldClickUrl() {
        basePageSteps.onCatalogPage().subHeader().url(tabTitle).click();
        urlSteps.fromUri(format("https://%s%s", urlSteps.getConfig().getBaseDomain(), tabUrl))
                .addParam("from", "top_menu_secondline").shouldNotSeeDiff();
    }
}
