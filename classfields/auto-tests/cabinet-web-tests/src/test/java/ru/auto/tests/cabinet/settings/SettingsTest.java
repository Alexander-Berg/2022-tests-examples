package ru.auto.tests.cabinet.settings;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.mock.MockDealerSettings.PROPERTY_ALLOW_PHOTO_REORDER;
import static ru.auto.tests.desktop.mock.MockDealerSettings.PROPERTY_AUTO_ACTIVATE_CARS_OFFERS;
import static ru.auto.tests.desktop.mock.MockDealerSettings.PROPERTY_AUTO_ACTIVATE_COMMERCIAL_OFFERS;
import static ru.auto.tests.desktop.mock.MockDealerSettings.PROPERTY_AUTO_ACTIVATE_MOTO_OFFERS;
import static ru.auto.tests.desktop.mock.MockDealerSettings.PROPERTY_HIDE_LICENSE_PLATE;
import static ru.auto.tests.desktop.mock.MockDealerSettings.mockDealerSettings;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_SETTINGS;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.ACTIVATE_OFFER_AFTER_IDLE_TIME_SECTION;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.AUTOMATIC_PHOTO_ORDERING_SECTION;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.AUTOMATIC_PHOTO_ORDERING_SWITCHER;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.CARS_SWITCHER;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.COM_TC_SWITCHER;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.DATA_SAVED_POPUP;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.HIDE_LICENSE_PLATE;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.HIDE_LICENSE_PLATE_SWITCHER;
import static ru.auto.tests.desktop.page.cabinet.CabinetSettingsSubscriptionsPage.MOTO_SWITCHER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Настройки")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SettingsTest {

    private static final int TIMEOUT = 6;

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
    public String switcher;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String requestPropertyName;

    @Parameterized.Parameters(name = "{index}: Переключатель «{0}» в блоке «{1}»")
    public static Object[][] getParameters() {
        return new Object[][] {
                {CARS_SWITCHER, ACTIVATE_OFFER_AFTER_IDLE_TIME_SECTION, PROPERTY_AUTO_ACTIVATE_CARS_OFFERS},
                {COM_TC_SWITCHER, ACTIVATE_OFFER_AFTER_IDLE_TIME_SECTION, PROPERTY_AUTO_ACTIVATE_COMMERCIAL_OFFERS},
                {MOTO_SWITCHER, ACTIVATE_OFFER_AFTER_IDLE_TIME_SECTION, PROPERTY_AUTO_ACTIVATE_MOTO_OFFERS},
                {AUTOMATIC_PHOTO_ORDERING_SECTION, AUTOMATIC_PHOTO_ORDERING_SWITCHER, PROPERTY_ALLOW_PHOTO_REORDER},
                {HIDE_LICENSE_PLATE, HIDE_LICENSE_PLATE_SWITCHER, PROPERTY_HIDE_LICENSE_PLATE}
        };
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerTariff"),
                stub("cabinet/ApiSubscriptionsClient"),

                stub("cabinet/DesktopSalonSilentPropertyUpdate"),

                stub().withPutDeepEquals(DEALER_SETTINGS)
                        .withRequestBody(
                                mockDealerSettings().setPropertyRequest(requestPropertyName, true).getRequestBody()
                        )
                        .withStatusSuccessResponse(),

                stub().withPutDeepEquals(DEALER_SETTINGS)
                        .withRequestBody(
                                mockDealerSettings().setPropertyRequest(requestPropertyName, false).getRequestBody()
                        )
                        .withStatusSuccessResponse()
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SETTINGS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Переключатели в блоке «Активировать объявления после простоя»")
    public void shouldSeeSwitcherActivationCars() {
        switchOn(section, switcher);
        steps.onCabinetSettingsPage().notifier().waitUntil(not(isDisplayed()), TIMEOUT);
        switchOff(section, switcher);
    }

    @Step("Выключаем переключатель «{switcher}»")
    private void switchOff(String section, String switcher) {
        steps.onCabinetSettingsPage().section(section).activeToggle(switcher).click();
        steps.onCabinetSettingsPage().notifier().waitUntil(isDisplayed()).should(hasText(DATA_SAVED_POPUP));
        steps.onCabinetSettingsPage().section(section).inactiveToggle(switcher).waitUntil(isDisplayed());
    }

    @Step("Включаем переключатель «{switcher}»")
    private void switchOn(String section, String switcher) {
        steps.onCabinetSettingsPage().section(section).inactiveToggle(switcher).click();
        steps.onCabinetSettingsPage().notifier().waitUntil(isDisplayed()).should(hasText(DATA_SAVED_POPUP));
        steps.onCabinetSettingsPage().section(section).activeToggle(switcher).waitUntil(isDisplayed());
    }
}
