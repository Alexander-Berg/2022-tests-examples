package ru.auto.tests.desktop.mag;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAG;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Журнал в листинге")
@Feature(MAG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MagListingTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().footer(), 0, 0);
        basePageSteps.onListingPage().mag().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.setWideWindowSize();

        basePageSteps.onListingPage().mag().title().should(isDisplayed()).hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_MAG)
                .addParam("from", "listing")
                .addParam("utm_campaign", "listing_journal")
                .addParam("utm_content", "block-mag-title")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по статье")
    public void shouldClickArticle() {
        String articleTitle = basePageSteps.onListingPage().mag().getArticle(0).title().getText();
        basePageSteps.onListingPage().mag().getArticle(0).hover().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onBasePage().title()
                .should(hasAttribute("textContent", containsString(articleTitle.trim().substring(0, 5))));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Больше материалов»")
    public void shouldClickMoreArticlesUrl() {
        basePageSteps.onListingPage().mag().moreArticlesUrl().should(isDisplayed()).hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_MAG)
                .addParam("from", "listing")
                .addParam("utm_campaign", "listing_journal_main")
                .addParam("utm_content", "block-mag-read-more")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }
}
