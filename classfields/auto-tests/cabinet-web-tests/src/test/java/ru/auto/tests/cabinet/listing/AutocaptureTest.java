package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.RECYCLE_ACTIVE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Сниппет активного объявления. Автоприменение")
@Story("Листинг объявлений дилера")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AutocaptureTest {

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {CARS},
                {TRUCKS},
                {MOTO}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/DesktopClientsGet/Dealer"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/UserOffersCarsUsed"),
                stub("cabinet/UserOffersTrucksUsed"),
                stub("cabinet/UserOffersMotoUsed"),
                stub("cabinet/BillingSchedulesCarsBoostPut"),
                stub("cabinet/BillingSchedulesTrucksBoostPut"),
                stub("cabinet/BillingSchedulesMotoBoostPut"),
                stub("cabinet/BillingSchedulesCarsBoostDelete"),
                stub("cabinet/BillingSchedulesTrucksBoostDelete"),
                stub("cabinet/BillingSchedulesMotoBoostDelete"),
                stub("cabinet/BillingSchedulesCarsAllSaleFreshPut"),
                stub("cabinet/BillingSchedulesCarsAllSaleFreshDelete"),
                stub("cabinet/BillingSchedulesTrucksAllSaleFreshPut"),
                stub("cabinet/BillingSchedulesTrucksAllSaleFreshDelete"),
                stub("cabinet/BillingSchedulesMotoAllSaleFreshPut"),
                stub("cabinet/BillingSchedulesMotoAllSaleFreshDelete")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(category).path(USED).addParam(STATUS, "active").open();
        steps.onCabinetOffersPage().snippet(0).waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @DisplayName("Поп-ап «Настроить автоприменение»")
    @Owner(TIMONDL)
    public void shouldSeeSetAutocapturePopup() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().hover();

        steps.onCabinetOffersPage().autocapturePopup()
                .should(hasText("Автоприменение\nПн\nВт\nСр\nЧт\nПт\nСб\nВс\n--:--\nСохранить расписание"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Поп-ап «Настроить автоприменение» после настройки услуги")
    public void shouldSeeSetAutocapturePopupAfterApplyingService() {
        steps.applyAutocapture(0);
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().hover();

        steps.onCabinetOffersPage().autocapturePopup().should(hasText("Автоприменение\nПн\nВт\nСр\nЧт\nПт\nСб\nВс" +
                "\nВ 01:00 по Москве\nСохранить расписание\nОтключить"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопка «Настроить автоприменение» после применения услуги")
    public void shouldAutocaptureButtonAfterApplyingService() {
        steps.applyAutocapture(0);

        steps.onCabinetOffersPage().snippet(0).serviceButtons().autocaptureIcon()
                .should(hasClass(containsString(RECYCLE_ACTIVE)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Подключение и отключение услуги «Автоприменение»")
    public void shouldActivateAndCancelAutocapture() {
        steps.applyAutocapture(0);
        steps.onCabinetOffersPage().snippet(0).serviceButtons().fresh().hover();
        steps.onCabinetOffersPage().autocapturePopup().button("Отключить").click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Автоприменение услуги «Поднятие в поиске» отключено"));
        steps.onCabinetOffersPage().autocapturePopup().should(not(isDisplayed()));

        steps.onCabinetOffersPage().snippet(0).serviceButtons().autocaptureIcon()
                .should(not(hasClass(containsString(RECYCLE_ACTIVE))));
    }
}
