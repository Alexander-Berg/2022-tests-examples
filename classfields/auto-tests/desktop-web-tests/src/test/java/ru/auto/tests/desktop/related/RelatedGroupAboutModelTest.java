package ru.auto.tests.desktop.related;

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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Похожие на группе новых - о модели")
@Feature(AutoruFeatures.RELATED)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class RelatedGroupAboutModelTest {

    private static final String MARK = "kia";
    private static final String MODEL = "optima";
    private static final String PATH = "/21342050-21342121/";
    private static final String FIRST_SALE = "/bmw/3er/20548423-20548433/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextListing",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsTechInfo",
                "desktop/ReferenceCatalogCarsTechParam",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/SearchCarsRelated").post();

        basePageSteps.setWideWindowSize();
        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path(PATH).open();
        basePageSteps.onGroupPage().tab("О модели").click();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение похожих")
    public void shouldSeeRelated() {
        basePageSteps.onGroupPage().verticalRelated().itemsList().should(hasSize(3))
                .forEach(item -> item.should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickRelatedSale() {
        basePageSteps.onGroupPage().verticalRelated().getItem(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).path(FIRST_SALE).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Обновить»")
    public void shouldClickUpdateButton() {
        String firstOfferUrl = basePageSteps.onGroupPage().verticalRelated().getItem(0).url().getAttribute("href");
        basePageSteps.onGroupPage().verticalRelated().updateButton().hover().click();
        basePageSteps.onGroupPage().verticalRelated().getItem(0).url()
                .waitUntil(not(hasAttribute("href", firstOfferUrl)));
    }
}
