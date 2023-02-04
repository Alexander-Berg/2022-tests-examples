package ru.auto.tests.cabinet.card;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Notifications.DATA_SUCCESSFULLY_SAVED;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARD;
import static ru.auto.tests.desktop.consts.Pages.DETAILS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.BIK;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.CONTACT_PERSON;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.EMAIL;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.FULL_NAME;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.INN;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.JURIDICAL_ADDRESS;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.KPP;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.NAME;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.OGRN;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.PATRONYMIC;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.PAYMENT_ACCOUNT;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.PHONE_CLASS;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.POSTAL_ADDRESS;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.POSTAL_CODE;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.SAVE_CHANGES;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.SHORT_NAME;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.SURNAME;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Реквизиты салона")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CardRequisitesTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("desktop/SearchCarsBreadcrumbs"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerRequisitesGet"),
                stub("cabinet/DesktopSalonPropertiesUpdateLegal"),
                stub("cabinet/DesktopSalonPropertiesUpdatePersonal")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CARD).path(DETAILS).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Изменение данных в блоке «Юридическое лицо»")
    public void shouldChangeDataLegal() {
        basePageSteps.onCabinetSalonCardPage().requisitesList().get(0).click();
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(FULL_NAME, "Полное название");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(SHORT_NAME, "Краткое название");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(INN, "123456789012");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(KPP, "123456789");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(JURIDICAL_ADDRESS, "Москва и другие города");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(OGRN, "123456789012345");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(BIK, "044525225");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(PAYMENT_ACCOUNT, "40817810938160925982");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(EMAIL, "demo@auto.ru");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(CONTACT_PERSON, "avto");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(PHONE_CLASS, "84950010397");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(POSTAL_CODE, "413865");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().input(POSTAL_ADDRESS, "москва и другие города");
        basePageSteps.onCabinetSalonCardPage().requisitesLegalPersonBlock().button(SAVE_CHANGES).click();

        basePageSteps.onCabinetSalonCardPage().notifier(DATA_SUCCESSFULLY_SAVED).should(isDisplayed());
    }

    @Test
    @Category({Regression.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Изменение данных в блоке «Физическое лицо»")
    public void shouldChangeDataPersonal() {
        basePageSteps.onCabinetSalonCardPage().requisitesList().get(1).click();
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(NAME, "Имя");
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(SURNAME, "Фамилия");
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(PATRONYMIC, "Отчество");
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(EMAIL, "demo@auto.ru");
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(PHONE_CLASS, "84950010397");
        basePageSteps.unfocusElement(basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(PHONE_CLASS));
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().button(SAVE_CHANGES).click();

        basePageSteps.onCabinetSalonCardPage().notifier(DATA_SUCCESSFULLY_SAVED).should(isDisplayed());
    }

}
