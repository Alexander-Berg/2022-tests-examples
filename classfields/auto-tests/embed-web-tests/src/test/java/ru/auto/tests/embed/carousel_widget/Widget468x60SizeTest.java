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

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES_WIGET;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.BANNER;
import static ru.auto.tests.desktop.consts.Pages.EMBED;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_PROMO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES_WIGET)
@DisplayName("EMBED: виджет карусели с объявлениями 468x60")
@Owner(NATAGOLOVKINA)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)

public class Widget468x60SizeTest {

    private static final String SIZE = "468x60";
    private static final String OFFER_PATH = "/cars/new/group/vaz/granta/21377675/21379385/1098326394-7cb2f37c/";
    private static final int TIMEOUT = 5;


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.newMock().with("embed/SearchCarsAll").post();

        urlSteps.subdomain(SUBDOMAIN_PROMO).path(EMBED).path(BANNER).path(SIZE)
                .addParam("_debug_embed", "true").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Скролл карусели каждые 4 сек")
    @Owner(NATAGOLOVKINA)
    public void shouldScrollCarousel() {
        basePageSteps.onCarouselWigetPage().title().waitUntil(hasText("BMW"), TIMEOUT); //0 - LADA (ВАЗ); 1 - bmw
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    @Owner(NATAGOLOVKINA)
    public void shouldClickSale() {
        basePageSteps.onCarouselWigetPage().getItem(0).should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(OFFER_PATH)
                .addParam("utm_campaign", "widget")
                .addParam("utm_content", "banner_" + SIZE).shouldNotSeeDiff();
    }

}