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

@DisplayName("Безопасная сделка. Форма. Блок «Данные об автомобиле»")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DealFormSellerVehicleInfoTest {

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
                "desktop-lk/SafeDealDealGetWithDocuments",
                "desktop/SafeDealDealUpdateVehicleInfo").post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заполняем блок с информацией про автомобиль под продавцом")
    public void shouldFillVehicleInfoBlockBySeller() {
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Тип ТС (легковой, грузовой, автобус)", "Легковой");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Рабочий объем, куб.см.", "3000");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Модель двигателя", "model-1");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Номер двигателя", "D123ff");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Номер шасси, рамы", "A123ss");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Номер кузова", "B123qq");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Паспорт ТС, серия/номер", "40 НН 111111");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Паспорт ТС, дата выдачи", "10.01.2010");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Паспорт ТС, кем выдан", "Москва");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("Гос. номер", "А111АА111");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("СТС, серия/номер", "1234567890");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("СТС, дата выдачи", "10.01.2010");
        basePageSteps.onDealPage().section("Данные об автомобиле").input("СТС, кем выдан", "Москва");

        mockRule.overwriteStub(2, "desktop-lk/SafeDealDealGetWithDocumentsAndVehicleInfo");

        basePageSteps.onDealPage().section("Данные об автомобиле").button("Подтвердить").click();

        basePageSteps.onDealPage().notifier().should(hasText("Данные сохранены"));
        basePageSteps.onDealPage().section("Банковские реквизиты").should(hasText("Банковские реквизиты\nШаг 3 из 4.\n " +
                "Подождите, пока покупатель укажет стоимость\nВы сможете указать реквизиты банковского счета, как " +
                "только согласуете стоимость автомобиля с покупателем\nСвязаться с покупателем"));
    }
}
