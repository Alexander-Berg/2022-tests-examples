package ru.auto.tests.mobile.main;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;

@DisplayName("Расширенные фильтры - тэги")
@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
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
    public String section;

    @Parameterized.Parameter(1)
    public String sectionUrl;

    @Parameterized.Parameter(2)
    public String paramName;

    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameter(4)
    public String paramQueryName;

    @Parameterized.Parameter(5)
    public String paramQueryValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2} {3} {4} {5} {6} {7}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Все", ALL, "Продавец", "Частник", "seller_group", "PRIVATE"},
                {"Все", ALL, "Владельцев по ПТС", "1 владелец", "owners_count_group", "ONE"},
                {"Все", ALL, "Срок владения", "До года", "owning_time_group", "LESS_THAN_YEAR"},
                {"Все", ALL, "Состояние", "Неважно", "damage_group", "ANY"},
                {"Все", ALL, "Таможня", "Неважно", "customs_state_group", "DOESNT_MATTER"},
                {"Все", ALL, "Дополнительные параметры", "Онлайн-показ", "online_view", "true"},
                {"Все", ALL, "Дополнительные параметры", "На гарантии", "with_warranty", "true"},
                {"Все", ALL, "Дополнительные параметры", "Обмен", "exchange_group", "POSSIBLE"},
                {"Все", ALL, "Дополнительные параметры", "С фото", "has_image", "false"},
                {"Все", ALL, "Дополнительные параметры", "С видео", "has_video", "true"},
                {"Все", ALL, "Дополнительные параметры", "С панорамой", "search_tag", "external_panoramas"},
                {"Все", ALL, "Дополнительные параметры", "Без доставки", "with_delivery", "NONE"},
                {"Все", ALL, "Дополнительные параметры", "Оригинал ПТС", "pts_status", "1"},
                {"Все", ALL, "Время размещения", "За сутки", "top_days", "1"},

                {"Новые", NEW, "Дополнительные параметры", "С фото", "has_image", "false"},
                {"Новые", NEW, "Дополнительные параметры", "С видео", "has_video", "true"},
                {"Новые", NEW, "Дополнительные параметры", "В наличии", "in_stock", "IN_STOCK"},
                {"Новые", NEW, "Дополнительные параметры", "С панорамой", "search_tag", "external_panoramas"},
                {"Новые", NEW, "Дополнительные параметры", "Онлайн-показ", "online_view", "true"},

                {"С пробегом", USED, "Дополнительные параметры", "С фото", "has_image", "false"},
                {"С пробегом", USED, "Дополнительные параметры", "С видео", "has_video", "true"},
                {"С пробегом", USED, "Дополнительные параметры", "Обмен", "exchange_group", "POSSIBLE"},
                {"С пробегом", USED, "Дополнительные параметры", "Оригинал ПТС", "pts_status", "1"},
                {"С пробегом", USED, "Дополнительные параметры", "На гарантии", "with_warranty", "true"},
                {"С пробегом", USED, "Дополнительные параметры", "Без доставки", "with_delivery", "NONE"},
                {"С пробегом", USED, "Дополнительные параметры", "Проверено производителем", "search_tag", "certificate_manufacturer"},
                {"С пробегом", USED, "Дополнительные параметры", "С панорамой", "search_tag", "external_panoramas"},
                {"С пробегом", USED, "Дополнительные параметры", "Онлайн-показ", "online_view", "true"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
    }

    @Test
    @DisplayName("Тэги")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTag() {
        basePageSteps.onMainPage().paramsPopup().section(section).click();
        basePageSteps.onMainPage().paramsPopup().tags(paramName).button(paramValue).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(sectionUrl)
                .addParam(paramQueryName, paramQueryValue).shouldNotSeeDiff();
    }
}
