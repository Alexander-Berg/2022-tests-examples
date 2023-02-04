package ru.auto.tests.desktop.listing.filters;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - расширенный фильтр - чекбоксы")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdvancedFiltersCheckboxesCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    //@Parameter("Ссылка")
    @Parameterized.Parameter
    public String path;

    //@Parameter("Название чекбокса")
    @Parameterized.Parameter(1)
    public String checkboxTitle;

    //@Parameter("Параметр")
    @Parameterized.Parameter(2)
    public String param;

    //@Parameter("Значение параметра")
    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/cars/new/", "С фото", "has_image", "false"},
                {"/cars/new/", "С видео", "has_video", "true"},
                {"/cars/new/", "В наличии", "in_stock", "IN_STOCK"},
                {"/cars/new/", "С панорамой", "search_tag", "external_panoramas"},
                {"/cars/new/", "Онлайн-показ", "online_view", "true"},

                {"/cars/audi/used/", "Оригинал ПТС", "pts_status", "1"},
                {"/cars/audi/used/", "На гарантии", "with_warranty", "true"},
                {"/cars/audi/used/", "Обмен", "exchange_group", "POSSIBLE"},
                {"/cars/audi/used/", "С фото", "has_image", "false"},
                {"/cars/audi/used/", "С видео", "has_video", "true"},
                {"/cars/audi/used/", "Без доставки", "with_delivery", "NONE"},
                {"/cars/audi/used/", "С панорамой", "search_tag", "external_panoramas"},

                {"/cars/all/", "Без доставки", "with_delivery", "NONE"},
                {"/cars/all/", "С панорамой", "search_tag", "external_panoramas"},
                {"/cars/all/", "Онлайн-показ", "online_view", "true"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор параметра-чекбокса")
    public void shouldSeeCheckboxParamInUrl() {
        urlSteps.testing().path(RUSSIA).path(path).open();
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().getSale(0), 0, 0);
        basePageSteps.onListingPage().filter().checkbox(checkboxTitle).should(isDisplayed()).click();
        urlSteps.addParam(param, paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}