package ru.auto.tests.desktop.group;

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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.element.SortBar.SortBy.PRICE_ASC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@DisplayName("Сортировки на группе")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GroupSortRefreshTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextGroupPriceAsc",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsGroupComplectations",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechParam").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Обновление списка предложений после смены сортировки")
    public void shouldRefreshOffersList() throws InterruptedException {
        String firstOfferUrl = basePageSteps.onGroupPage().getOffer(0).additionalInfoUrl().getAttribute("href");
        basePageSteps.scrollDown(300);
        basePageSteps.onGroupPage().selectItem("Сортировка", PRICE_ASC.getName());
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onGroupPage().getOffer(0).additionalInfoUrl()
                .should(not(hasAttribute("href", firstOfferUrl)));
    }
}