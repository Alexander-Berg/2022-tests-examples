package ru.auto.tests.mobile.footer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FOOTER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Футер")
@Feature(FOOTER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FooterSaleTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение футера")
    public void shouldSeeFooter() {
        basePageSteps.onCardPage().footer().waitUntil(isDisplayed()).should(hasText("Дилерам\nО проекте\nПомощь" +
                "\nАналитика Авто.ру\nСаша Котов\nПриложение Авто.ру\nПолная версия\nСтань частью команды" +
                "\nАвто.ру — один из самых посещаемых автомобильных сайтов в российском интернете\nМы" +
                " предлагаем большой выбор легковых автомобилей, грузового и коммерческого транспорта, " +
                "мототехники, спецтехники и многих других видов транспортных средств\n© 1996–2022 ООО «Яндекс.Вертикали»"
        ));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Ссылка «Полная версия»")
    public void shouldClickDesktopUrl() {
        basePageSteps.hideElement(basePageSteps.onCardPage().floatingContacts());
        basePageSteps.onCardPage().footer().button("Полная версия").hover().click();
        urlSteps.desktopURI().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/").path(SALE_ID)
                .addParam("nomobile", "true").shouldNotSeeDiff();
        cookieSteps.shouldSeeCookieWithValue("nomobile", "1");
    }
}
