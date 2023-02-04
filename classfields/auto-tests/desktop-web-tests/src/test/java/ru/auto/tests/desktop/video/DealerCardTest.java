package ru.auto.tests.desktop.video;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@DisplayName("Видео на карточке дилера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealerCardTest {

    private static final String MARK = "Mercedes-Benz";
    private static final String MARK_CODE = "mercedes";
    private static final String MODEL = "GLA";
    private static final String MODEL_CODE = "gla_class";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsRid213",
                "desktop/SearchCarsBreadcrumbsMercedes",
                "desktop/SessionUnauth",
                "desktop/Salon",
                "desktop/SearchCarsCountDealerId",
                "desktop/SearchCarsMarkModelFiltersAllDealerIdOneMark",
                "desktop/SearchCarsAllDealerId",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, 1024);
        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL).path(CARS_OFFICIAL_DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по вкладке «Видео» после выбора марки, модели")
    @Category({Regression.class, Testing.class})
    public void shouldClickVideoTab() {
        basePageSteps.onDealerCardPage().filter().selectItem(MARK, "Любая");
        basePageSteps.onDealerCardPage().filter().selectItem("Марка", MARK);
        basePageSteps.onDealerCardPage().filter().selectItem("Модель", MODEL);
        basePageSteps.onDealerCardPage().subHeader().button("Видео").waitUntil(isDisplayed()).click();
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK_CODE).path(MODEL_CODE).path("/").shouldNotSeeDiff();
    }
}