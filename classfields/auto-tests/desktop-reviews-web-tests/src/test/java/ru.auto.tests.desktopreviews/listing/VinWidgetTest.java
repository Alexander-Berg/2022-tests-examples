package ru.auto.tests.desktopreviews.listing;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.yandex.qatools.htmlelements.matchers.common.HasTextMatcher.hasText;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.VIN_WIDGET)
@DisplayName("Листинг отзывов - виджет проверки по VIN")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VinWidgetTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {MOTO},
                {TRUCKS}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).path(category).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Отображение виджета")
    public void shouldSeeWidget() {
        basePageSteps.onReviewsListingPage().vinWidget().should(hasText("История автомобиля\nПокажет скрученный пробег, " +
                "ДТП, предыдущие размещения на Авто.ру и другое.\nГосномер или VIN\nНайти"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск по госномеру")
    @Category({Regression.class})
    public void shouldSearchByPlateNumber() {
        String plateNumber = "a555aa54";
        basePageSteps.onReviewsListingPage().vinWidget().input("Госномер или VIN", plateNumber);
        basePageSteps.onReviewsListingPage().vinWidget().button("Найти").click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(HISTORY).path(plateNumber.toUpperCase()).path("/")
                .addParam("from", "widget.vin.autoru").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск по VIN")
    @Category({Regression.class})
    public void shouldSearchByVin() {
        String vin = "JN1TANY62U0017600";
        basePageSteps.onReviewsListingPage().vinWidget().input("Госномер или VIN", vin);
        basePageSteps.onReviewsListingPage().vinWidget().button("Найти").click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(HISTORY).path(vin).path("/").addParam("from", "widget.vin.autoru")
                .shouldNotSeeDiff();
    }
}