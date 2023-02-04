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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAG;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_NARROW_PAGE;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Журнал")
@Feature(MAG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MagTeaserMainTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);
        urlSteps.testing().open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.onMainPage().magTeaser().title().should(isDisplayed()).hover().click();
        urlSteps.subdomain(SUBDOMAIN_MAG)
                .addParam("from", "banner")
                .addParam("utm_campaign", "glavnaya_top")
                .addParam("utm_content", "block-mag-title")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
        basePageSteps.onMagPage().header().waitUntil(isDisplayed());
        basePageSteps.onMagPage().header().button("Разместить бесплатно").waitUntil(isDisplayed());
        basePageSteps.onMagPage().footer().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по статье")
    public void shouldClickArticle() {
        mockRule.deleteCookies();

        String articeTitle = basePageSteps.onMainPage().magTeaser().getArticle(0).title().getText();
        basePageSteps.onMainPage().magTeaser().getArticle(0).hover().click();
        basePageSteps.onBasePage().title().should(hasAttribute("textContent", startsWith(articeTitle.substring(0, 5))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Все материалы»")
    public void shouldClickAllArticlesButton() {
        basePageSteps.onMainPage().magTeaser().allArticlesButton().should(isDisplayed()).hover().click();
        urlSteps.subdomain(SUBDOMAIN_MAG)
                .addParam("from", "banner")
                .addParam("utm_campaign", "glavnaya_top")
                .addParam("utm_content", "block-mag-read-more")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ресайзинг")
    public void shouldResize() {
        basePageSteps.setWindowSize(WIDTH_NARROW_PAGE, HEIGHT_1024);
        basePageSteps.onMainPage().magTeaser().should(not(isDisplayed()));

        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);
        basePageSteps.onMainPage().magTeaser().waitUntil(isDisplayed());
    }
}
