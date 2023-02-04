package ru.auto.tests.mobile.filters;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Расширенные фильтры - тэги")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsTagsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

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
    public String paramValue;

    @Parameterized.Parameter(4)
    public String paramQueryName;

    @Parameterized.Parameter(5)
    public String paramQueryValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Продавец", "Частник", "seller_group", "PRIVATE"},
                {CARS, ALL, "Владельцев по ПТС", "1 владелец", "owners_count_group", "ONE"},
                {CARS, ALL, "Срок владения", "До года", "owning_time_group", "LESS_THAN_YEAR"},
                {CARS, ALL, "Состояние", "Неважно", "damage_group", "ANY"},
                {CARS, ALL, "Таможня", "Неважно", "customs_state_group", "DOESNT_MATTER"},
                {CARS, ALL, "Дополнительные параметры", "Онлайн-показ", "online_view", "true"},
                {CARS, ALL, "Дополнительные параметры", "На гарантии", "with_warranty", "true"},
                {CARS, ALL, "Дополнительные параметры", "Обмен", "exchange_group", "POSSIBLE"},
                {CARS, ALL, "Дополнительные параметры", "С фото", "has_image", "false"},
                {CARS, ALL, "Дополнительные параметры", "С видео", "has_video", "true"},
                {CARS, ALL, "Дополнительные параметры", "С панорамой", "search_tag", "external_panoramas"},
                {CARS, ALL, "Дополнительные параметры", "Без доставки", "with_delivery", "NONE"},
                {CARS, ALL, "Дополнительные параметры", "Оригинал ПТС", "pts_status", "1"},
                {CARS, ALL, "Дополнительные параметры", "Продажа с НДС", "only_nds", "true"},
                {CARS, ALL, "Время размещения", "За сутки", "top_days", "1"},

                {CARS, NEW, "Дополнительные параметры", "С фото", "has_image", "false"},
                {CARS, NEW, "Дополнительные параметры", "С видео", "has_video", "true"},
                {CARS, NEW, "Дополнительные параметры", "В наличии", "in_stock", "IN_STOCK"},
                {CARS, NEW, "Дополнительные параметры", "С панорамой", "search_tag", "external_panoramas"},
                {CARS, NEW, "Дополнительные параметры", "Онлайн-показ", "online_view", "true"},

                {CARS, USED, "Дополнительные параметры", "С фото", "has_image", "false"},
                {CARS, USED, "Дополнительные параметры", "С видео", "has_video", "true"},
                {CARS, USED, "Дополнительные параметры", "Обмен", "exchange_group", "POSSIBLE"},
                {CARS, USED, "Дополнительные параметры", "Оригинал ПТС", "pts_status", "1"},
                {CARS, USED, "Дополнительные параметры", "На гарантии", "with_warranty", "true"},
                {CARS, USED, "Дополнительные параметры", "Без доставки", "with_delivery", "NONE"},
                {CARS, USED, "Дополнительные параметры", "Проверено производителем", "search_tag", "certificate_manufacturer"},
                {CARS, USED, "Дополнительные параметры", "С панорамой", "search_tag", "external_panoramas"},
                {CARS, USED, "Дополнительные параметры", "Онлайн-показ", "online_view", "true"},

                {LCV, ALL, "Продавец", "Частник", "seller_group", "PRIVATE"},
                {LCV, ALL, "Состояние", "Неважно", "damage_group", "ANY"},
                {LCV, ALL, "Таможня", "Растаможен", "customs_state_group", "CLEARED"},
                {LCV, ALL, "Дополнительные параметры", "В наличии", "in_stock", "IN_STOCK"},
                {LCV, ALL, "Дополнительные параметры", "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {LCV, ALL, "Дополнительные параметры", "В наличии", "in_stock", "IN_STOCK"},
                {LCV, ALL, "Дополнительные параметры", "Обмен", "exchange_group", "POSSIBLE"},
                {LCV, ALL, "Дополнительные параметры", "Продажа с НДС", "only_nds", "true"},
                {LCV, ALL, "Дополнительные параметры", "С фото", "has_image", "false"},
                {LCV, ALL, "Дополнительные параметры", "Без доставки", "with_delivery", "NONE"},
                {LCV, ALL, "Время размещения", "За сутки", "top_days", "1"},

                {AGRICULTURAL, ALL, "Дополнительные параметры", "В наличии", "in_stock", "IN_STOCK"},
                {AGRICULTURAL, ALL, "Дополнительные параметры", "Обмен", "exchange_group", "POSSIBLE"},
                {AGRICULTURAL, ALL, "Дополнительные параметры", "Продажа с НДС", "only_nds", "true"},
                {AGRICULTURAL, ALL, "Дополнительные параметры", "С фото", "has_image", "false"},
                {AGRICULTURAL, ALL, "Дополнительные параметры", "Без доставки", "with_delivery", "NONE"},

                {MOTORCYCLE, ALL, "Продавец", "Частник", "seller_group", "PRIVATE"},
                {MOTORCYCLE, ALL, "Состояние", "Неважно", "damage_group", "ANY"},
                {MOTORCYCLE, ALL, "Таможня", "Растаможен", "customs_state_group", "CLEARED"},
                {MOTORCYCLE, ALL, "Дополнительные параметры", "В наличии", "in_stock", "IN_STOCK"},
                {MOTORCYCLE, ALL, "Дополнительные параметры", "Продажа с НДС", "only_nds", "true"},
                {MOTORCYCLE, ALL, "Дополнительные параметры", "Обмен", "exchange_group", "POSSIBLE"},
                {MOTORCYCLE, ALL, "Дополнительные параметры", "С фото", "has_image", "false"},
                {MOTORCYCLE, ALL, "Время размещения", "За сутки", "top_days", "1"},
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
    }

    @Test
    @DisplayName("Тэги")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTag() {
        basePageSteps.onListingPage().paramsPopup().tags(paramName).button(paramValue).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.addParam(paramQueryName, paramQueryValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
        urlSteps.refresh();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
    }
}
