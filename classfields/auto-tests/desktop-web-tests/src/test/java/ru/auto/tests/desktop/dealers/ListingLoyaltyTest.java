package ru.auto.tests.desktop.dealers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALER_CARD;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILERY;
import static ru.auto.tests.desktop.consts.Pages.LOYALTY;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Проверенный дилер")
@Feature(DEALERS)
@Story(DEALER_CARD)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ListingLoyaltyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/AutoruBreadcrumbsNewUsed"),
                stub("desktop/AutoruDealerDealerId"),
                stub("desktop/SalonPhones")
        ).create();

        urlSteps.testing().path(RUSSIA).path(DILERY).path(CARS).path(ALL)
                .addParam("dealer_id", "20699478").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Поп-ап «Проверенный дилер»")
    public void shouldSeeLoyaltyPopup() {
        basePageSteps.onDealerListingPage().getDealer(0).hover();
        basePageSteps.onDealerListingPage().getDealer(0).loyaltyIcon().should(hasText("Проверенный дилер")).hover();
        basePageSteps.onDealerListingPage().popup().waitUntil(hasText("Статус «Проверенный дилер»\nвыдается автосалонам, " +
                "которые:\nУспешно прошли проверку нашей службой модерации\nБольше полугода продают машины на Авто.ру\n" +
                "Публикуют только актуальные объявления\nПодробнее о проверенных дилерах"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке в поп-апе")
    public void shouldClickLoyaltyPopupButton() {
        basePageSteps.onDealerListingPage().getDealer(0).hover();
        basePageSteps.onDealerListingPage().getDealer(0).loyaltyIcon().waitUntil(isDisplayed()).hover();
        basePageSteps.onDealerListingPage().popup().button("Подробнее о проверенных дилерах")
                .waitUntil(isDisplayed()).click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(PROMO).path(LOYALTY).shouldNotSeeDiff();
    }
}