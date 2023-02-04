package ru.auto.tests.mobile.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка дилера - параметры - теги")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CardParamsTagsNotOfficialTest {

    private static final String DEALER_CODE = "/inchcape_certified_moskva/";

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
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String paramName;

    @Parameterized.Parameter(3)
    public String paramQueryName;

    @Parameterized.Parameter(4)
    public String paramQueryValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Онлайн-показ", "online_view", "true"},
                {CARS, ALL, "На гарантии", "with_warranty", "true"},
                {CARS, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {CARS, ALL, "С фото", "has_image", "false"},
                {CARS, ALL, "С видео", "has_video", "true"},
                {CARS, ALL, "С панорамой", "search_tag", "external_panoramas"},
                {CARS, ALL, "Без доставки", "with_delivery", "NONE"},
                {CARS, ALL, "Оригинал ПТС", "pts_status", "1"},

                {CARS, NEW, "С фото", "has_image", "false"},
                {CARS, NEW, "С видео", "has_video", "true"},
                {CARS, NEW, "В наличии", "in_stock", "IN_STOCK"},
                {CARS, NEW, "С панорамой", "search_tag", "external_panoramas"},
                {CARS, NEW, "Онлайн-показ", "online_view", "true"},

                {CARS, USED, "С фото", "has_image", "false"},
                {CARS, USED, "С видео", "has_video", "true"},
                {CARS, USED, "Обмен", "exchange_group", "POSSIBLE"},
                {CARS, USED, "Оригинал ПТС", "pts_status", "1"},
                {CARS, USED, "На гарантии", "with_warranty", "true"},
                {CARS, USED, "Без доставки", "with_delivery", "NONE"},
                {CARS, USED, "Проверено производителем", "search_tag", "certificate_manufacturer"},
                {CARS, USED, "С панорамой", "search_tag", "external_panoramas"},
                {CARS, USED, "Онлайн-показ", "online_view", "true"},
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SalonNotOfficial",
                "desktop/SearchCarsCountDealerIdNotOfficial",
                "desktop/SearchCarsMarkModelFiltersAllDealerIdSeveralMarks",
                "desktop/SearchCarsMarkModelFiltersNewDealerIdSeveralMarks",
                "desktop/SearchCarsMarkModelFiltersUsedDealerIdSeveralMarks",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(DILER).path(category).path(section).path(DEALER_CODE).open();
        basePageSteps.scrollAndClick(basePageSteps.onDealerCardPage().filters().paramsButton());
        basePageSteps.hideApplyFiltersButton();
    }

    @Test
    @DisplayName("Тэги")
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTag() {
        basePageSteps.onDealerCardPage().paramsPopup().button(paramName).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onDealerCardPage().paramsPopup().applyFiltersButton().click();
        urlSteps.addParam(paramQueryName, paramQueryValue).shouldNotSeeDiff();
        basePageSteps.onDealerCardPage().filters().should(isDisplayed());
    }
}
