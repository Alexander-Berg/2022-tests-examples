package ru.auto.tests.desktop.video;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@DisplayName("Видео в листинге дилеров")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealersListingTest {

    private static final String MARK = "Audi";
    private static final String UNPOPULAR_MARK = "Lamborghini";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по вкладке «Видео» после выбора марки в новых")
    @Category({Regression.class})
    public void shouldClickVideoTabInNew() {
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(NEW).open();
        basePageSteps.onDealerListingPage().searchBlock().mark(MARK).click();
        basePageSteps.onDealerListingPage().subHeader().button("Видео").waitUntil(isDisplayed()).click();
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK.toLowerCase()).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Вкладка «Видео» должна пропадать после выбора непопулярной марки")
    @Category({Regression.class})
    public void shouldNotSeeVideoTab() {
        urlSteps.testing().path(MOSKVA).path(DILERY).path(CARS).path(UNPOPULAR_MARK.toLowerCase()).open();
        basePageSteps.onListingPage().subHeader().button("Видео").should(not(isDisplayed()));
    }
}