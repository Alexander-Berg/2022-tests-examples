package ru.auto.tests.desktop.listing;

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
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - «Новые автомобили в наличии у официальных дилеров»")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class NewCarsFromDealersTest {

    private static final int SALES_FROM_DEALERS_POSITION = 17;
    private static final int VISIBLE_ITEMS_CNT = 4;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsAll",
                "desktop/SearchCarsContextRecommendNewInStock").post();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
        screenshotSteps.setWindowSize(1440, 5000);
        basePageSteps.onListingPage().getSale(SALES_FROM_DEALERS_POSITION).hover();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока")
    public void shouldSeeSalesFromDealers() {
        basePageSteps.onListingPage().salesFromDealers().title()
                .should(hasText("Новые автомобили в наличии у официальных дилеров\nСмотреть все"));
        basePageSteps.onListingPage().salesFromDealers().itemsList().subList(0, VISIBLE_ITEMS_CNT).forEach(item -> {
                    item.should(isDisplayed());
                    item.image().should(isDisplayed());
                }
        );
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Смотреть все»")
    public void shouldClickSeeAllButton() {
        basePageSteps.onListingPage().salesFromDealers().button("Смотреть все").hover().click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).addParam("geo_radius", "200").
                fragment("priceRange").shouldNotSeeDiff();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Листание объявлений")
    public void shouldSlideSales() {
        basePageSteps.onListingPage().salesFromDealers().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> item.waitUntil(isDisplayed()));
        basePageSteps.onListingPage().salesFromDealers().prevButton().should(not(isDisplayed()));
        basePageSteps.onListingPage().salesFromDealers().nextButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().salesFromDealers().itemsList().get(VISIBLE_ITEMS_CNT)
                .should(isDisplayed());
        basePageSteps.onListingPage().salesFromDealers().prevButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().salesFromDealers().itemsList().subList(0, VISIBLE_ITEMS_CNT)
                .forEach(item -> isDisplayed());
        basePageSteps.onListingPage().salesFromDealers().prevButton().should(not(isDisplayed()));
        basePageSteps.onListingPage().salesFromDealers().nextButton().should(isDisplayed());

    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        basePageSteps.onListingPage().salesFromDealers().getItem(0).should(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.shouldUrl(startsWith(urlSteps.testing().path(MOSKVA).path(CARS).path(NEW).path(GROUP).toString()));
    }
}
