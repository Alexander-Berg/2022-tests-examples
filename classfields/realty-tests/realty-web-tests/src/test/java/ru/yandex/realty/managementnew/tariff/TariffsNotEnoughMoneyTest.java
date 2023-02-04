package ru.yandex.realty.managementnew.tariff;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.TARIFFS;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.element.management.StickyTariff.CHOOSE_TARIFF;
import static ru.yandex.realty.element.management.StickyTariff.EXTENDED_BLOCK;
import static ru.yandex.realty.element.management.StickyTariff.MAXIMUM_BLOCK;
import static ru.yandex.realty.element.management.TariffPopup.CHOOSE_THIS_TARIFF;

@Tag(JURICS)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница тарифа")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TariffsNotEnoughMoneyTest {

    private static final String MESSAGE = "На счёте недостаточно средств. Мы не можем провести оплату по звонкам " +
            "в рамках вашего тарифа. Ваши объявления не продвигаются в выдаче умным алгоритмом продвижения. " +
            "Пополните баланс личного кабинета, чтобы получать больше звонков.";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Parameterized.Parameter
    public String tariff;

    @Parameterized.Parameters(name = "Видим сообщение что не хватает денег {0}")
    public static Collection<String> testParameters() {
        return asList(
                EXTENDED_BLOCK,
                MAXIMUM_BLOCK
        );
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSeeNotEnoughMoneyExtended() {
        apiSteps.createRealty3JuridicalAccount(account);
        offerBuildingSteps.addNewOffer(account).create();
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().stickyTariff().block(EXTENDED_BLOCK).button(CHOOSE_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().button(CHOOSE_THIS_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().should(not(exists()));
        managementSteps.onTariffsPage().animated().waitUntil(not(exists()));
        managementSteps.refreshUntil(() ->
                managementSteps.onManagementNewPage().notification(format(MESSAGE, tariff)), isDisplayed());
    }
}
