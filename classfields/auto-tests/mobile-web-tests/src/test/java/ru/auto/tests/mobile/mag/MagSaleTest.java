package ru.auto.tests.mobile.mag;

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

import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAG;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Журнал на карточке объявления")
@Feature(MAG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
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
        mockRule.newMock().with("desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onCardPage().footer(), 0, 0);
        basePageSteps.onCardPage().mag().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().mag().title());
        urlSteps.subdomain(SUBDOMAIN_MAG)
                .addParam("utm_campaign", "card_journal_mobile")
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
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().mag().getArticle(0));
        urlSteps.shouldUrl(startsWith(urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).toString()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Читать журнал»")
    public void shouldClickReadMagButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().mag().button("Читать журнал"));
        urlSteps.subdomain(SUBDOMAIN_MAG)
                .addParam("utm_campaign", "card_journal_mobile")
                .addParam("utm_content", "block-mag-read-more")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }
}
