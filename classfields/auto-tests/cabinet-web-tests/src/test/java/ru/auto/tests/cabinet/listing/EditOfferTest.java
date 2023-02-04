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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CONTACTS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.STATUS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Сниппет активного объявления. Редактирование оффера")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class EditOfferTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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

    @Parameterized.Parameter(1)
    public String editPath;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {CARS, "beta/cars/used/"},
                {TRUCKS, TRUCKS},
                {MOTO, MOTO}
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
                stub("cabinet/UserOffersTrucksUsed"),
                stub("cabinet/UserOffersMotoUsed"),
                stub("cabinet/UserOffersCarsHide"),
                stub("cabinet/UserOffersTrucksHide"),
                stub("cabinet/UserOffersMotoHide"),
                stub("cabinet/UserOffersCarsDelete"),
                stub("cabinet/UserOffersTrucksDelete"),
                stub("cabinet/UserOffersMotoDelete")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(category).path(USED).addParam(STATUS, "active")
                .open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопка «Редактировать»")
    public void shouldSeeOfferEditPage() {
        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Редактировать").click();

        steps.switchToNextTab();
        urlSteps.testing().path(editPath).path(EDIT).path(SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Кнопка «Реквизиты»")
    public void shouldSeeContactsEditPage() {
        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Реквизиты").click();

        steps.switchToNextTab();
        urlSteps.testing().path(category).path(CONTACTS).path(EDIT).path(SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Удаление оффера")
    public void shouldDeleteOffer() {
        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Удалить").click();
        steps.onCabinetOffersPage().removePopup().button("Да").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявление удалено"));
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Отмена удаления оффера")
    public void shouldCancelOfferDeletion() {
        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Удалить").click();
        steps.onCabinetOffersPage().removePopup().button("Нет").click();
        steps.onCabinetOffersPage().removePopup().should(not(isDisplayed()));
        steps.onCabinetOffersPage().snippet(0).should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Снятие с продажи")
    public void shouldHideOffer() {
        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Снять с\u00a0продажи").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявление снято с продажи"));
    }
}
