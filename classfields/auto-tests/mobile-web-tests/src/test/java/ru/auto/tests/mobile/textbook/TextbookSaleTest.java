package ru.auto.tests.mobile.textbook;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.TEXTBOOK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.THEME;
import static ru.auto.tests.desktop.consts.Pages.UCHEBNIK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Учебник на карточке объявления")
@Feature(TEXTBOOK)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class TextbookSaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
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
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().footer().hover();

        mockRule.delete();
        mockRule.deleteCookies();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение статей")
    public void shouldSeeArticles() {
        basePageSteps.onCardPage().textbook().articlesList().should(hasSize(ARTICLES_CNT))
                .forEach(item -> item.waitUntil(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по статье")
    public void shouldClickArticle() {
        basePageSteps.onCardPage().textbook().getArticle(0).hover();
        String firstArticle = basePageSteps.onCardPage().textbook().getArticle(0).getText();
        basePageSteps.scrollDown(basePageSteps.onCardPage().textbook().getArticle(0).getSize().getHeight() * 2);
        basePageSteps.onCardPage().textbook().getArticle(0).click();
        basePageSteps.onMagPage().h1().should(hasText(firstArticle));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все»")
    public void shouldClickAllArticlesUrl() {
        basePageSteps.onCardPage().textbook().allArticlesUrl().should(isDisplayed()).hover();
        basePageSteps.scrollDown(150);
        basePageSteps.onCardPage().textbook().allArticlesUrl().click();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(THEME).path(UCHEBNIK)
                .addParam("from", "card")
                .addParam("utm_campaign", "card_uchebnik")
                .addParam("utm_content", "block-uchebnik-read-more")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }
}
