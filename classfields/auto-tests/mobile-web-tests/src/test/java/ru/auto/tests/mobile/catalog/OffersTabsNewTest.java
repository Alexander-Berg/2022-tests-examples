package ru.auto.tests.mobile.catalog;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Каталог - «Предложения о продаже» - вкладки")
@Feature(AutoruFeatures.CATALOG)
public class OffersTabsNewTest {

    private static final String MARK = "vaz";
    private static final String MODEL = "granta";
    private static final String GENERATION = "21377296";
    private static final String BODY = "21377429";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION).path("/").open();
        basePageSteps.onCatalogGenerationPage().footer().hover();
        basePageSteps.onCatalogGenerationPage().offers().waitUntil(isDisplayed());
        basePageSteps.onCatalogGenerationPage().offers().tab("Новые").click();
        basePageSteps.onCatalogGenerationPage().offers().itemsList().should(hasSize(greaterThan(0)))
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Testing.class})
    @DisplayName("Клик по кнопке «Смотреть все»")
    public void shouldClickShowAllButton() {
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCatalogGenerationPage().offers().allButton(), 0, -500);
        basePageSteps.onCatalogGenerationPage().offers().allButton().click();
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(anyOf(
                equalTo(urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL)
                        .path(format("/%s-%s/", GENERATION, BODY)).addParam("from", "single_group_snippet_listing").toString()),
                equalTo(urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL).path(NEW).toString())));
    }
}
