package ru.auto.tests.mag.mobile;

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
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAG;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.QueryParams.FROM;
import static ru.auto.tests.desktop.mobile.page.MagPage.TEST_ARTICLE;
import static ru.auto.tests.desktop.page.MagPage.MAG_AD_CAROUSEL;
import static ru.auto.tests.desktop.page.MagPage.MARK;
import static ru.auto.tests.desktop.page.MagPage.WITH_DRAFT_MODEL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Журнал - статья - объявления")
@Feature(MAG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class MagArticleSalesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(TEST_ARTICLE)
                .addParam(WITH_DRAFT_MODEL, "true").open();
        basePageSteps.onMagPage().tth().hover();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по заголовку")
    public void shouldClickTitle() {
        basePageSteps.scrollAndClick(basePageSteps.onMagPage().sales().title());
        urlSteps.switchToNextTab();

        urlSteps.mobileURI().path(MOSKVA).path(CARS).path(ALL).addParam(FROM, MAG_AD_CAROUSEL).shouldNotSeeDiff();
        basePageSteps.onCardPage().title()
                .should(hasAttribute("textContent", startsWith("Купить ")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        basePageSteps.scrollAndClick(basePageSteps.onMagPage().sales().getSale(0));
        urlSteps.switchToNextTab();

        urlSteps.shouldUrl(endsWith(format("/?%s=%s", FROM, MAG_AD_CAROUSEL)));
        basePageSteps.onCardPage().h1().should(hasText(startsWith(MARK.toUpperCase())));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все {mark} на Авто.ру»")
    public void shouldClickMoreSalesUrl() {
        basePageSteps.scrollAndClick(basePageSteps.onMagPage().sales().buttonContains(
                format("Все %s на Авто.ру", MARK.toUpperCase())));
        urlSteps.switchToNextTab();

        urlSteps.mobileURI().path(MOSKVA).path(CARS).path(MARK).path(ALL).addParam(FROM, MAG_AD_CAROUSEL)
                .shouldNotSeeDiff();
        basePageSteps.onCardPage().titleTag().should(hasAttribute("textContent", startsWith("Купить ")));
    }
}
