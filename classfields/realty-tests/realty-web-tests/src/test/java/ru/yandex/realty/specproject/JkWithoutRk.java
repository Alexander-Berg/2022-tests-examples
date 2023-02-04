package ru.yandex.realty.specproject;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.FROM_SPECIAL;
import static ru.yandex.realty.step.UrlSteps.SAMOLET_VALUE;

@DisplayName("Спецпроект. РК")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class JkWithoutRk {

    private static final String TEST_JK = "/pribrezhnyj-park-2651324/";
    private static final String SIMILAR_JK = "ЖК в продаже, похожие на «Прибрежный Парк»";
    private static final String PAYED_JK = "/rumyancevo-park-1544302/";
    private static final String REGION_JK = "/tarskaya-krepost-658761/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private NewBuildingSteps newBuildingSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(TEST_JK);
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот страницы без спец проекта")
    public void shouldSeePageScreenshotWithoutSpec() {
        newBuildingSteps.disableAd();
        compareSteps.resize(1920, 8000);
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(newBuildingSteps.onBasePage().pageContent());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(newBuildingSteps.onBasePage().pageContent());
        compareSteps.screenshotsShouldBeTheSame(testing, production, 39);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Скриншот страницы со спец проектом")
    public void shouldSeePageScreenshotWithSpec() {
        compareSteps.resize(1920, 8000);
        urlSteps.queryParam(FROM_SPECIAL, SAMOLET_VALUE).open();
        Screenshot testing = compareSteps.takeScreenshot(newBuildingSteps.onBasePage().pageContent());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(newBuildingSteps.onBasePage().pageContent());
        compareSteps.screenshotsShouldBeTheSame(testing, production, 39);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Есть шорткаты, доп галереи и хода строительства")
    public void shouldSeeExtraBlocks() {
        urlSteps.queryParam(FROM_SPECIAL, SAMOLET_VALUE).open();
        newBuildingSteps.onNewBuildingSitePage().progressPhotos().should(hasSize(greaterThan(0)));
        newBuildingSteps.onNewBuildingSitePage().shortcut("").should(isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().progressBlock().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Карточки ЖК в регионах без ЦЗ не обрезаются. Платный ЖК в регионе ЦЗ")
    public void shouldSeeExtraBlocksPayedJk() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(PAYED_JK).open();
        newBuildingSteps.onNewBuildingSitePage().progressPhotos().should(hasSize(greaterThan(0)));
        newBuildingSteps.onNewBuildingSitePage().shortcut("").should(isDisplayed());
        newBuildingSteps.onNewBuildingSitePage().progressBlock().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Карточки ЖК в регионах без ЦЗ не обрезаются. ЖК в регионе без ЦЗ")
    public void shouldSeeExtraBlocksRegionJk() {
        urlSteps.testing().path("/omsk/").path(KUPIT).path(NOVOSTROJKA).path(REGION_JK).open();
        newBuildingSteps.onNewBuildingSitePage().similarJkInSales("").should(isDisplayed());
    }

}
