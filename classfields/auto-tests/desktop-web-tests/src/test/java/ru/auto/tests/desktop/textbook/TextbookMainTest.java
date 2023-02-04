package ru.auto.tests.desktop.textbook;

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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.TEXTBOOK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.THEME;
import static ru.auto.tests.desktop.consts.Pages.UCHEBNIK;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_NARROW_PAGE;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Учебник на главной")
@Feature(TEXTBOOK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class TextbookMainTest {

    private static final int ARTICLES_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().open();
        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.onMainPage().textbook().title().should(isDisplayed()).hover().click();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(THEME).path(UCHEBNIK)
                .addParam("from", "index")
                .addParam("utm_campaign", "glavnaya_uchebnik")
                .addParam("utm_content", "block-uchebnik-title")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение статей")
    public void shouldSeeArticles() {
        basePageSteps.onMainPage().textbook().articlesList().should(hasSize(ARTICLES_CNT))
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по статье")
    public void shouldClickArticle() {
        String firstArticle = basePageSteps.onMainPage().textbook().getArticle(0).getText();
        cookieSteps.deleteCookie("mockritsa_imposter");
        basePageSteps.onMainPage().textbook().getArticle(0).click();
        basePageSteps.onMagPage().h1().should(hasText(firstArticle));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все статьи»")
    public void shouldClickAllArticlesUrl() {
        basePageSteps.onMainPage().textbook().allArticlesUrl().should(isDisplayed()).hover().click();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(THEME).path(UCHEBNIK)
                .addParam("from", "index")
                .addParam("utm_campaign", "glavnaya_uchebnik")
                .addParam("utm_content", "block-uchebnik-read-more")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ресайзинг")
    public void shouldResizeTextbook() {
        basePageSteps.setWindowSize(WIDTH_NARROW_PAGE, HEIGHT_1024);
        basePageSteps.onMainPage().textbook().getArticle(3).should(not(isDisplayed()));

        basePageSteps.setWindowSize(WIDTH_WIDE_PAGE, HEIGHT_1024);
        basePageSteps.onMainPage().textbook().getArticle(3).waitUntil(isDisplayed());
    }
}