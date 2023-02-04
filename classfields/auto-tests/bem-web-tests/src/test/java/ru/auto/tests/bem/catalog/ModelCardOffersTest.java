package ru.auto.tests.bem.catalog;

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
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка модели - «Предложения о продаже»")
@Feature(AutoruFeatures.CATALOG)
public class ModelCardOffersTest {

    private static final String MARK = "bmw";
    private static final String MODEL = "x5";
    private static final int VISIBLE_SIMILAR_OFFERS = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).open();
        basePageSteps.onCatalogPage().cardGenerations().hover();
        basePageSteps.scrollDown(basePageSteps.onCatalogPage().cardGenerations().getSize().getHeight());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Листание предложений")
    public void shouldScrollOffers() {
        basePageSteps.onCatalogPage().offers().itemsList().subList(0, VISIBLE_SIMILAR_OFFERS)
                .forEach(i -> isDisplayed());
        basePageSteps.onCatalogPage().offers().prevButton().should(not(isDisplayed()));
        basePageSteps.onCatalogPage().offers().nextButton().click();
        basePageSteps.onCatalogPage().offers().itemsList().subList(0, VISIBLE_SIMILAR_OFFERS)
                .forEach(i -> isDisplayed());
        basePageSteps.onCatalogPage().offers().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCatalogPage().offers().itemsList().subList(0, VISIBLE_SIMILAR_OFFERS)
                .forEach(i -> isDisplayed());
        basePageSteps.onCatalogPage().offers().prevButton().should(not(isDisplayed()));
        basePageSteps.onCatalogPage().offers().nextButton().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по предложению")
    public void shouldClickOffer() {
        basePageSteps.onCatalogPage().offers().getItem(0).click();
        basePageSteps.switchToNextTab();
        basePageSteps.onCardPage().cardHeader().title()
                .waitUntil(hasText(startsWith(format("%s %s", MARK.toUpperCase(), MODEL.toUpperCase()))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Смотреть все»")
    public void shouldClickShowAllUrl() {
        basePageSteps.onCatalogPage().offers().allUrl()
                .should(hasAttribute("href", urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL)
                        .path(ALL).path("/body-allroad_5_doors/").toString())).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}