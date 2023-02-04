package ru.auto.tests.cabinet.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.SERVICE_CANCELED;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.element.cabinet.ServiceButtons.SPEC_ACTIVE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Услуга «Спец»")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SpecServiceTest {

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

    @Inject
    private ScreenshotSteps screenshotSteps;

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
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerInfoMultipostingDisabled"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/UserOffersCarsUsed"),
                stub("cabinet/UserOffersCarsProductsSpecialPost"),
                stub("cabinet/UserOffersCarsProductsSpecialDelete"),
                stub("cabinet/UserOffersTrucksUsed"),
                stub("cabinet/UserOffersTrucksProductsSpecialPost"),
                stub("cabinet/UserOffersTrucksProductsSpecialDelete"),
                stub("cabinet/UserOffersMotoUsed"),
                stub("cabinet/UserOffersMotoProductsSpecialPost"),
                stub("cabinet/UserOffersMotoProductsSpecialDelete")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(category).path(USED).addParam(STATUS, "active").open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(TIMONDL)
    @DisplayName("Нотификация с подробным описанием услуги")
    public void shouldSeeSpecialNotificationPopup() {
        steps.onCabinetOffersPage().snippet(0).serviceButtons().special().hover();

        steps.onCabinetOffersPage().popup().should(hasText("Спецпредложение\n" +
                "Ваше объявление будет отображаться в специальном блоке на страницах поисковой выдачи по " +
                "марке/модели и на страницах Каталога, а также иметь приоритетную позицию в блоке «Похожие» " +
                "на карточках объявлений о продаже аналогичных авто.\nПодключить за 120 ₽ в день"));
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопка «Спец» после применения")
    public void shouldSeeSpecButton() {
        steps.applySpecService(0);

        steps.onCabinetOffersPage().snippet(0).serviceButtons().special()
                .should(hasClass(containsString(SPEC_ACTIVE)));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Подключаем и отключаем услугу «Спец»")
    public void shouldActivateAndCancelService() {
        steps.applySpecService(0);
        steps.onCabinetOffersPage().snippet(0).serviceButtons().special().click();

        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText(SERVICE_CANCELED));
        steps.onCabinetOffersPage().snippet(0).serviceButtons().special()
                .should(not(hasClass(containsString(SPEC_ACTIVE))));
    }
}
