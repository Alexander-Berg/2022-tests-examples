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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ABOUT;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.THEME;

@DisplayName("Журнал на группе новых")
@Feature(AutoruFeatures.MAG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class MagGroupAboutModelTest {

    private static final String MARK = "kia";
    private static final String MODEL = "optima";
    private static final String PATH = "/21342050-21342121/";
    private static final String ARTICLE_KIA = "k5vsoptimatest/";

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
        mockRule.newMock().with("mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReferenceCatalogCarsConfigurationsGallery",
                "desktop/PostsKiaOptima").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(MARK).path(MODEL).path(PATH).path(ABOUT).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.onGroupAboutModelPage().mag().title().click();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(THEME).path("/kia-optima/")
                .addParam("utm_campaign", "card_group_journal_mobile")
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
        basePageSteps.onGroupAboutModelPage().mag().getArticle(0).click();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(ARTICLE_KIA).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Все статьи»")
    public void shouldClickAllArticlesButton() {
        basePageSteps.onGroupAboutModelPage().mag().button("Все статьи").click();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(THEME).path("/kia-optima/")
                .addParam("utm_campaign", "card_group_journal_mobile")
                .addParam("utm_content", "block-mag-read-more")
                .addParam("utm_medium", "cpm")
                .addParam("utm_source", "auto-ru")
                .shouldNotSeeDiff();
    }
}
