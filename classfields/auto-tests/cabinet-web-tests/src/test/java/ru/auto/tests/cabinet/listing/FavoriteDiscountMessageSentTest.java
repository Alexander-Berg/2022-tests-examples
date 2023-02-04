package ru.auto.tests.cabinet.listing;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Отправка сообщения о скидке")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class FavoriteDiscountMessageSentTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/DesktopClientsGet/Dealer"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/UserOffersCars/ActiveWithSentFavoriteDiscount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/InciteGetFavoriteMessage")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Шторка с отправленным сообщением про скидку")
    public void shouldOpenFavoriteDiscountMessageCurtain() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().favorites().click();

        steps.onCabinetOffersPage().favoriteDiscountsCurtain().should(hasText("Получили сообщение\n1" +
                "\nПредложение для 1 пользователя\nСкидка, %\nСкидка, ₽\nПодарок\nРазмер скидки, %\nСрок действия\n" +
                "\nКодовое слово\nСообщение, которое получат покупатели\nCitroen C5\n463 698 ₽  515 220 ₽" +
                "\nПозвоните по объявлению в течение 2 часов, чтобы получить скидку 10%. Скажите менеджеру кодовое " +
                "слово «QA». Предложение действует, пока не найдётся первый покупатель."));
    }
}
