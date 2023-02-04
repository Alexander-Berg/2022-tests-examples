package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.Pages;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LOYALTY;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(SALES)
@DisplayName("Объявление - проверенный дилер")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ContactsDealerLoyaltyTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedDealer").post();

        urlSteps.testing().path(CARS).path(USED).path(Pages.SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Иконка «Проверенный дилер» в контактах")
    public void shouldSeeLoyaltyIcon() {
        basePageSteps.onCardPage().contacts().loyaltyIcon().hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Статус «Проверенный дилер»\n" +
                "выдается автосалонам, которые:\nУспешно прошли проверку нашей службой модерации\n" +
                "Больше полугода продают машины на Авто.ру\nПубликуют только актуальные объявления\n" +
                "Подробнее о проверенных дилерах"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Иконка «Проверенный дилер» в плавающей панели")
    public void shouldSeeStickyPanelLoyalty() {
        basePageSteps.scrollDown(1000);
        basePageSteps.onCardPage().stickyBar().waitUntil(isDisplayed());
        basePageSteps.onCardPage().stickyBar().loyaltyIcon().hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Статус «Проверенный дилер»\n" +
                "выдается автосалонам, которые:\nУспешно прошли проверку нашей службой модерации\n" +
                "Больше полугода продают машины на Авто.ру\nПубликуют только актуальные объявления\n" +
                "Подробнее о проверенных дилерах"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке в поп-апе")
    public void shouldClickLoyaltyPopupButton() {
        basePageSteps.onCardPage().contacts().loyaltyIcon().hover();
        basePageSteps.onCardPage().popup().button("Подробнее о проверенных дилерах")
                .click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(PROMO).path(LOYALTY).shouldNotSeeDiff();
    }
}