package ru.auto.tests.embed.carousel_widget;

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

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES_WIGET;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EMBED;
import static ru.auto.tests.desktop.consts.Pages.LISTING_CAROUSEL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_PROMO;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES_WIGET)
@DisplayName("EMBED: виджет карусели с объявлениями для Журнала")
@Owner(NATAGOLOVKINA)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)

public class WidgetListingCarouselTest {

    private static final String MARK = "NISSAN";
    private static final String MODEL = "QASHQAI";
    private static final String OFFER = "/1099693044-4370027c/";
    private static final String MARK_MODEL = format("mark=%s,model=%s", MARK.toUpperCase(), MODEL.toUpperCase());

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Rule
    @com.google.inject.Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.newMock().with("embed/SearchCarsMarkModel").post();

        urlSteps.subdomain(SUBDOMAIN_PROMO).path(EMBED).path(LISTING_CAROUSEL)
                .addParam("catalog_filter", MARK_MODEL)
                .addParam("_debug_embed", "true").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    @Owner(NATAGOLOVKINA)
    public void shouldClickSale() {
        basePageSteps.onCarouselListingPage().getItem(0).url().click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(OFFER)
                .addParam("from", "widget-listing-carousel")
                .addParam("utm_source", "yandex-zen")
                .addParam("utm_campaign", "widget-listing-carousel").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке «Больше объявлений на Авто.ру»")
    @Owner(NATAGOLOVKINA)
    public void shouldClickListingLink() {
        basePageSteps.onCarouselWigetPage().button("Больше объявлений на Авто.ру")
                .should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase()).path(ALL)
                .addParam("from", "autoru-widget-carousel-listing")
                .addParam("utm_source", "yandex-zen")
                .addParam("utm_campaign", "widget-listing-carousel")
                .shouldNotSeeDiff();
    }
}