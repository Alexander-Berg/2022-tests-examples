package ru.auto.tests.mobile.deal;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Безопасная сделка. Форма. Блок «Личная встреча и подтверждение сделки»")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DealFormBuyerDealApproveTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop-lk/SafeDealDealGetWithBuyerDealDocuments",
                "desktop/SafeDealDealUpdateBuyerRequestCode").post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подтверждаем всю сделку под покупателем")
    public void shouldApproveDealByBuyer() {
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").button("Запросить код").click();
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").input("Код из смс", "123456");

        mockRule.overwriteStub(2, "desktop-lk/SafeDealDealGetWithApprovedBuyer");
        mockRule.overwriteStub(3, "desktop/SafeDealDealUpdateBuyerEnterCode");
        basePageSteps.onDealPage().section("Личная встреча и подтверждение сделки").button("Подтвердить").click();

        basePageSteps.onDealPage().notifier().should(hasText("Данные сохранены"));
        basePageSteps.onDealPage().success().should(hasText("Сделка успешно завершена!\nНе забудьте забрать у " +
                "продавца ключи от автомобиля, ПТС, СТС, а также 2 экземпляра договора.\nДеньги за автомобиль " +
                "поступят на счет продавца в ближайшее время."));
    }
}
