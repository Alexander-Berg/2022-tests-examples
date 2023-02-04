package ru.auto.tests.desktop.vas;

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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.DesktopConfig.LISTING_TOP_SALES_CNT;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - объявления с услугой «Топ»")
@Feature(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class TopSalesListingMotoTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchMotoAll",
                "desktop/SearchMotoBreadcrumbsEmpty").post();

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение объявлений с услугой ТОП")
    public void shouldSeeTopSales() {
        basePageSteps.onListingPage().topSalesList().should(hasSize(LISTING_TOP_SALES_CNT))
                .forEach(item -> item.topIcon().should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поп-ап с описанием услуги")
    public void shouldSeePopup() {
        basePageSteps.onListingPage().getTopSale(0).topIcon().should(isDisplayed()).hover();
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed()).should(hasText("Поднятие в ТОП\n" +
                "Ваше объявление окажется в специальном блоке на самом верху списка при сортировке по актуальности " +
                "или по дате. Покупатели вас точно не пропустят.\n15\nУвеличивает количество просмотров в 15 раз\n" +
                "Подключить у себя"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка в поп-апе")
    public void shouldClickPopupUrl() {
        basePageSteps.onListingPage().getTopSale(0).topIcon().waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().activePopupLink().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MY).path(MOTO).addParam("from", "listing")
                .addParam("vas_service", "top").shouldNotSeeDiff();
    }
}