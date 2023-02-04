package ru.yandex.realty.managementnew.tariff;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.UrlSteps;

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
import static ru.yandex.realty.element.management.StickyTariff.MINIMUM_BLOCK;
import static ru.yandex.realty.element.management.StickyTariff.YOUR_CURRENT_TARIFF_BUTTON;
import static ru.yandex.realty.element.management.TariffPopup.CHOOSE_CURRENT_TARIFF;

@Tag(JURICS)
@Link("https://st.yandex-team.ru/VERTISTEST-1525")
@DisplayName("Страница тарифа")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class PopupTariffsPageTest {

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

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем попап -> тариф не применяется. Закрытие крестиком")
    public void shouldSeeClosePopupByCross() {
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(CHOOSE_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().iconTypeCross().click();
        managementSteps.onTariffsPage().tariffPopup().should(not(exists()));
        managementSteps.onTariffsPage().stickyTariff().block(MAXIMUM_BLOCK).button(CHOOSE_TARIFF).should(isDisplayed());
        managementSteps.onTariffsPage().stickyTariff().block(MINIMUM_BLOCK).button(YOUR_CURRENT_TARIFF_BUTTON)
                .should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Закрываем попап -> тариф не применяется. Закрытие кнопкой")
    public void shouldSeeClosePopupByButton() {
        urlSteps.testing().path(MANAGEMENT_NEW).path(TARIFFS).open();
        managementSteps.onTariffsPage().stickyTariff().block(EXTENDED_BLOCK).button(CHOOSE_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().button(CHOOSE_CURRENT_TARIFF).click();
        managementSteps.onTariffsPage().tariffPopup().should(not(exists()));
        managementSteps.onTariffsPage().stickyTariff().block(EXTENDED_BLOCK).button(CHOOSE_TARIFF).should(isDisplayed());
        managementSteps.onTariffsPage().stickyTariff().block(MINIMUM_BLOCK).button(YOUR_CURRENT_TARIFF_BUTTON)
                .should(isDisplayed());
    }
}
