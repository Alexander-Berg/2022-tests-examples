package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.COOKIESYNC;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Подшапка")
@Feature(DEALERS)
@Story(DEALER_CARD)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardSubHeaderTest {

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
    public String section;

    @Parameterized.Parameter(1)
    public String title;

    @Parameterized.Parameter(2)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {USED, "Объявления", "https://%s/moskva/cars/used/"},
                {USED, "Каталог", "https://%s/catalog/cars/"},
                {USED, "Отзывы", "https://%s/reviews/"},
                {USED, "Видео", "https://%s/video/"},
                {NEW, "Объявления", "https://%s/moskva/cars/new/"},
                {NEW, "Дилеры", "https://%s/moskva/dilery/cars/new/"},
                {NEW, "Каталог", "https://%s/catalog/cars/"},
                {NEW, "Отзывы", "https://%s/reviews/"},
                {NEW, "Видео", "https://%s/video/"},
                {ALL, "Объявления", "https://%s/moskva/cars/all/"},
                {ALL, "Дилеры", "https://%s/moskva/dilery/cars/all/"},
                {ALL, "Каталог", "https://%s/catalog/cars/"},
                {ALL, "Отзывы", "https://%s/reviews/"},
                {ALL, "Видео", "https://%s/video/"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionUnauth"),
                stub("desktop/SearchCarsBreadcrumbsRid213"),
                stub("desktop/SearchCarsBreadcrumbsMercedes"),
                stub("desktop/Salon"),
                stub("desktop/AutoruDealerStateNewAndOrgType"),
                stub("desktop/AutoruDealerAll"),
                stub("desktop/AutoruBreadcrumbsStateNew"),
                stub("desktop/AutoruBreadcrumbsNewUsed")
        ).create();

        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE + 10, HEIGHT_1024);
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(section)
                .path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Вкладки")
    public void shouldClickTab() {
        basePageSteps.onDealerListingPage().subHeader().button(title).click();

        urlSteps.fromUri(format(url, urlSteps.getConfig().getBaseDomain()))
                .ignoreParam(COOKIESYNC)
                .shouldNotSeeDiff();
    }
}