package ru.auto.tests.cabinet.registration;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.CARD;
import static ru.auto.tests.desktop.consts.Pages.DETAILS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.ADD_REQUISITES;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.BIK;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.CONTACT_PERSON;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.EMAIL;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.FULL_NAME;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.INN;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.JURIDICAL_ADDRESS;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.KPP;
import static ru.auto.tests.desktop.element.cabinet.card.RequisitesBlock.LEGAL_PERSON;
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
import static ru.auto.tests.desktop.page.cabinet.CabinetSalonCardPage.SITE;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Регистрация")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class RegistrationTest {

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
                stub("cabinet/ApiClientManager404"),
                stub("cabinet/DealerAccountNew"),
                stub("cabinet/CommonCustomerGetNew"),
                stub("cabinet/DesktopClientsGetNew"),
                stub("cabinet/ApiClientRegistrationSteps"),
                stub("cabinet/DesktopSalonInfoGetNew"),
                stub("cabinet/DesktopSalonInfoUpdateNew"),
                stub("cabinet/DesktopSalonPropertiesGetNew"),
                stub("cabinet/ApiAgency"),
                stub("cabinet/DesktopSalonPropertiesUpdateLegalNew"),
                stub("cabinet/DesktopSalonPropertiesUpdatePersonalNew")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CARD).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Регистрация. Дилер - юрлицо")
    public void shouldRegisterDealer() {
        basePageSteps.onCabinetSalonCardPage().input(SITE, "http://www.major-expert.ru");
        basePageSteps.onCabinetSalonCardPage().inputAddress().sendKeys("улица Льва Толстого, 12с1");
        basePageSteps.onCabinetSalonCardPage().button(SAVE_CHANGES).click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CARD).path(DETAILS).shouldNotSeeDiff();
        basePageSteps.onCabinetSalonCardPage().button(ADD_REQUISITES).click();

        basePageSteps.onCabinetSalonCardPage().radioButton(LEGAL_PERSON).click();
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

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Регистрация. Частник")
    public void shouldRegisterPhysical() {
        basePageSteps.onCabinetSalonCardPage().input(SITE, "http://www.major-expert.ru");
        basePageSteps.onCabinetSalonCardPage().inputAddress().sendKeys("улица Льва Толстого, 12с1");
        basePageSteps.onCabinetSalonCardPage().button(SAVE_CHANGES).click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CARD).path(DETAILS).shouldNotSeeDiff();
        basePageSteps.onCabinetSalonCardPage().button(ADD_REQUISITES).click();

        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(NAME, "Имя");
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(SURNAME, "Фамилия");
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(PATRONYMIC, "Отчество");
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(EMAIL, "demo@auto.ru");
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(PHONE_CLASS, "84950010397");
        basePageSteps.unfocusElement(basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().input(PHONE_CLASS));
        basePageSteps.onCabinetSalonCardPage().requisitesPhysicalBlock().button(SAVE_CHANGES).click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).shouldNotSeeDiff();
    }

}
