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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;

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
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Заблокированные объявления")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BannedOffersWithMultipleReasonsTest {

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
    public static Collection<String> getParameters() {
        return Arrays.asList(
                CARS,
                TRUCKS,
                MOTO
        );
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
                stub("cabinet/UserOffersCarsBannedWithTwoReasons"),
                stub("cabinet/UserOffersTrucksBannedWithTwoReasons"),
                stub("cabinet/UserOffersMotoBannedWithTwoReasons")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(category).path(USED).addParam(STATUS, "banned").open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(TIMONDL)
    @DisplayName("Не должно быть кнопки Активировать")
    public void shouldNotSeeActivateOfferButton() {
        steps.onCabinetOffersPage().snippet(0).saleButton().click();
        steps.onCabinetOffersPage().popupEditing().button("Активировать").should(not(isDisplayed()));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть плашку с причинами блокировки")
    public void shouldSeeBanReason() {
        steps.onCabinetOffersPage().snippet(0).banPlaceholder()
                .should(hasText("Объявление заблокировано по \nнескольким причинам"));
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(TIMONDL)
    @DisplayName("Поп-ап с причинами блокировки")
    public void shouldSeeBanReasonPopup() {
        steps.onCabinetOffersPage().snippet(0).banPlaceholder().reason().hover();

        steps.onCabinetOffersPage().activePopup().should(hasText("Модель ТС указана неверно\nДругая модификация" +
                "\nПроверьте, чтобы марки, модели и модификации ТС во всех объявлениях вашего салона были " +
                "заявлены без ошибок. Уточнить нужные характеристики можно в нашем каталоге.\nМарка ТС указана " +
                "неверно\nПроверьте, чтобы марки, модели и модификации ТС во всех объявлениях вашего салона были " +
                "заявлены без ошибок. Уточнить нужные характеристики можно в нашем каталоге."));
    }
}
