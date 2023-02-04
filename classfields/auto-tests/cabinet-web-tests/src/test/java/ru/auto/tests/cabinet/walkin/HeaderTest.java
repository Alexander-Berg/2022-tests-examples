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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.consts.Pages.WALKIN;
import static ru.auto.tests.desktop.consts.Pages.WALK_IN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Приезды в салон - заголовок страницы")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class HeaderTest {

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
                "cabinet/DealerTariff/CarsUsedOn").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALK_IN).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение блока с описанием страницы")
    public void shouldSeeTitleVisitCount() {
        steps.onCabinetWalkInPage().titleVisitCount().should(hasText("Приездов за период — 10 462"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение блока с описанием страницы")
    public void shouldSeeDescriptionBlock() {
        steps.onCabinetWalkInPage().descriptionBlock().waitUntil(isDisplayed());
        steps.onCabinetWalkInPage().descriptionBlock().text().should(hasText("На этой странице отображаются данные о " +
                "пользователях, которые посетили дилерский центр после просмотра ваших объявлений на Авто.ру"));
        steps.onCabinetWalkInPage().descriptionBlock().promoButton().should(hasText("Как это работает"));
        steps.onCabinetWalkInPage().descriptionBlock().cancelButton().should(hasText("Закрыть уведомление"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Закрытие блока с описанием страницы")
    public void shouldCloseDescriptionBlock() {
        steps.onCabinetWalkInPage().descriptionBlock().waitUntil(isDisplayed());
        steps.onCabinetWalkInPage().descriptionBlock().cancelButton().click();
        steps.onCabinetWalkInPage().descriptionBlock().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по кнопке «Как это работает»")
    public void shouldClickHelpButton() {
        steps.onCabinetWalkInPage().descriptionBlock().waitUntil(isDisplayed());
        steps.onCabinetWalkInPage().descriptionBlock().cancelButton().click();
        steps.onCabinetWalkInPage().helpButton().waitUntil(isDisplayed()).click();

        steps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(WALKIN).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по промо кнопке блока с описанием страницы")
    public void shouldClickPromoButton() {
        steps.onCabinetWalkInPage().descriptionBlock().waitUntil(isDisplayed());
        steps.onCabinetWalkInPage().descriptionBlock().promoButton().click();

        steps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(WALKIN).shouldNotSeeDiff();
    }
}
