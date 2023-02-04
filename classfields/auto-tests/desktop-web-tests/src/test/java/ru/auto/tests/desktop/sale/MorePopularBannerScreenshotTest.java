package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Баннер «Это объявление популярнее, чем ваше»")
@Feature(SALES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MorePopularBannerScreenshotTest {

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

    @Parameterized.Parameter
    public String color;

    @Parameterized.Parameter(1)
    public String text;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"blue", "Это объявление популярнее, чем ваше\n" +
                        "1820 человек искало эту модель сегодня\n35\nчеловек посмотрели это объявление\n26\nчеловек " +
                        "посмотрели ваше объявление\nВыбрать опцию для продвижения"},
                {"yellow", "Это объявление популярнее, чем ваше\n1820\nчеловек искало эту модель сегодня\n35\n" +
                        "человек посмотрели это объявление\nВыбрать опцию для продвижения"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionMorePopular",
                "desktop/AdTargetUserMorePopular",
                "desktop/OfferCarsMorePopular",
                "desktop/OfferCarsStatsMorePopular").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/1090110718-50e4924a/").open();
        waitForBanner();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KRISKOLU)
    @DisplayName("Отображение баннера")
    public void shouldSeeBanner() {
        basePageSteps.onCardPage().morePopularBanner(color).should(hasText(text));
    }

    @Step("Рефрешим страницу, пока не увидим нужный баннер")
    private void waitForBanner() {
        await().atMost(120, SECONDS).ignoreExceptions().pollInterval(3, SECONDS)
                .until(() -> {
                    basePageSteps.refresh();
                    return basePageSteps.onCardPage().morePopularBanner(color).isDisplayed();
                });
    }
}