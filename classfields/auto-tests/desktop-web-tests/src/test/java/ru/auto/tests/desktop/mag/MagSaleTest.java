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

import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAG;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_WIDE_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Журнал на карточке объявления")
@Feature(MAG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MagSaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().footer(), 0, 0);
        basePageSteps.onCardPage().mag().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.setWindowWidth(WIDTH_WIDE_PAGE);
        basePageSteps.onCardPage().mag().title().should(isDisplayed()).hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_MAG)
                .addParam("from", "card")
                .addParam("utm_campaign", "card_journal")
                .addParam("utm_content", "block-mag-title")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по статье")
    public void shouldClickArticle() {
        basePageSteps.onCardPage().mag().getArticle(0).hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(startsWith(urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).toString()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Больше материалов»")
    public void shouldClickMoreArticlesUrl() {
        basePageSteps.onCardPage().mag().moreArticlesUrl().should(isDisplayed()).hover().click();
        basePageSteps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_MAG)
                .addParam("from", "card")
                .addParam("utm_campaign", "card_journal_main")
                .addParam("utm_content", "block-mag-read-more")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }
}
