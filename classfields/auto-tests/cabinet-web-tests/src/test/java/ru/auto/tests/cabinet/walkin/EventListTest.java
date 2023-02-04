package ru.auto.tests.cabinet.walkin;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Pages.WALK_IN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Приезды в салон - список событий")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class EventListTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerWalkInStats",
                "cabinet/DealerWalkInEvents",
                "cabinet/DealerTariff/CarsUsedOn").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALK_IN).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение элемента из списка")
    public void shouldSeeEventItem() {
        steps.onCabinetWalkInPage().getEventItem(0).should(hasText("ПК\nFord Kuga, с пробегом\n" +
                "С пробегом Volvo XC90\n—"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по ссылке объявления")
    public void shouldClickOfferLink() {
        steps.onCabinetWalkInPage().getEventItem(0).getViewLink(0).waitUntil(isDisplayed()).click();
        steps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE)
                .path("/ford/kuga/1093491772-babfd575/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Разворачивание и сворачивание содержимого элемента из списка")
    public void shouldExpandEventItem() {
        steps.onCabinetWalkInPage().getEventItem(0).click();
        steps.onCabinetWalkInPage().getEventItem(0).waitUntil(hasText("ПК\nFord Kuga, с пробегом\n" +
                "С пробегом Volvo XC90\nС пробегом Chevrolet Equinox\nС пробегом Ford Kuga\nС пробегом Volvo XC60\n" +
                "С пробегом SsangYong Actyon\nС пробегом Nissan Patrol\nС пробегом Volvo XC70\nС пробегом Toyota Camry\n" +
                "С пробегом Skoda Octavia\nС пробегом Hyundai Grand Starex\n—"));
        steps.onCabinetWalkInPage().getEventItem(0).click();
        steps.onCabinetWalkInPage().getEventItem(0).waitUntil(hasText("ПК\nFord Kuga, с пробегом\n" +
                "С пробегом Volvo XC90\n—"));
    }
}
