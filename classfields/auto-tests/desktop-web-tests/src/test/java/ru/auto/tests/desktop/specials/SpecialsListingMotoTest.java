package ru.auto.tests.desktop.specials;

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
import static ru.auto.tests.desktop.consts.AutoruFeatures.SPECIAL;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - спецпредложения")
@Feature(SPECIAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SpecialsListingMotoTest {

    private static final int SPECIAL_SALES_POSITION = 13;
    private static final int ITEMS_CNT = 6;
    private static final int VISIBLE_ITEMS_CNT = 4;

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
        mockRule.newMock().with("desktop/SearchMotoBreadcrumbsEmpty",
                "desktop/SearchMotoAll",
                "desktop/SearchMotoSpecialsAll").post();

        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(ALL).open();
        basePageSteps.onListingPage().getSale(SPECIAL_SALES_POSITION).hover();
        basePageSteps.onListingPage().specialSales().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение спецпредложений")
    public void shouldSeeSpecialSales() {
        basePageSteps.onListingPage().specialSales().title().should(hasText("Спецпредложения"));
        basePageSteps.onListingPage().specialSales().itemsList().should(hasSize(ITEMS_CNT))
                .subList(0, VISIBLE_ITEMS_CNT).forEach(item -> item.should(isDisplayed()));
        basePageSteps.onListingPage().specialSales().getItem(0)
                .should(hasText("KTM RC 390\n2018, 9 685 км\n225 000 ₽"));
        basePageSteps.onListingPage().specialSales().nextButton().should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поп-ап с описанием услуги")
    public void shouldSeePopup() {
        basePageSteps.onListingPage().specialSales().how().hover();
        basePageSteps.onListingPage().activePopup().waitUntil(isDisplayed()).should(hasText("Спецпредложение\n" +
                "Ваше объявление будет отображаться в специальном блоке в результатах поиска и на карточках объявлений " +
                "о продаже аналогичных авто. А для легковых — также на главной странице и в Каталоге.\n5\n" +
                "Увеличивает количество просмотров в 5 раз\nПодключить у себя"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка в поп-апе")
    public void shouldClickPopupUrl() {
        basePageSteps.onListingPage().specialSales().how().waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().activePopupLink().waitUntil(isDisplayed()).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MY).path(MOTO).addParam("from", "specials_block")
                .addParam("vas_service", "special").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по объявлению")
    public void shouldClickSale() {
        mockRule.with("desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").update();

        basePageSteps.onListingPage().specialSales().getItem(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path("/ktm/rc_390/3531506-ff10142b/")
                .shouldNotSeeDiff();
    }
}