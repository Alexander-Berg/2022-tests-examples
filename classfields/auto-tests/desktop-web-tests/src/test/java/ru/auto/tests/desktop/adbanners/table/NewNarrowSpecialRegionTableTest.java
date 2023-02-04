package ru.auto.tests.desktop.adbanners.table;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.KURAU;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.TABLE;
import static ru.auto.tests.desktop.page.AdsPage.C1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author kurau (Yuri Kalinin)
 */
@DisplayName("Баннеры «CARS / NEW»")
@Epic(BANNERS)
@Feature(LISTING)
@Story("Узкий экран")
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class NewNarrowSpecialRegionTableTest {

    // https://bunker.yandex-team.ru/auto_ru/common/ad_special_rids?view=raw
    private static final String NOT_A_MOSCOW = "11162";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        cookieSteps.setRegion(NOT_A_MOSCOW);
        basePageSteps.setNarrowWindowSize();
        urlSteps.testing().path(CARS).path(NEW).addParam(OUTPUT_TYPE, TABLE).open();
    }

    @Test
    @Owner(KURAU)
    @Category({Regression.class})
    @DisplayName("Баннеры. CARS/NEW. Не Москва + Табличный вид")
    public void shouldOpenBannerCarsNewTableNarrow() {
        basePageSteps.scrollDown(200);
        basePageSteps.onAdsPage().ad(C1).should(isDisplayed()).click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
