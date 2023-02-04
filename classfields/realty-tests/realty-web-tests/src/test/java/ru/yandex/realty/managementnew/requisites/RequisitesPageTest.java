package ru.yandex.realty.managementnew.requisites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.mock.GetMoneyPersonTemplate;
import ru.yandex.realty.module.RealtyWebModuleWithoutDelete;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS_BILLING;
import static ru.yandex.realty.element.management.SettingsContent.ADDRESS_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.EMAIL_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.INDEX_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.INN_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.JURIDICAL_ADDRESS_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.KPP_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.PHONE_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.SAVE_CHANGES;
import static ru.yandex.realty.element.management.SettingsContent.TITLE_NAME_SECTION;
import static ru.yandex.realty.mock.GetMoneyPersonTemplate.getMoneyPersonTemplate;
import static ru.yandex.realty.page.ManagementNewPage.ADD_PAYER;

@DisplayName("Страница реквизитов")
@Feature(MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutDelete.class)
public class RequisitesPageTest {

    private GetMoneyPersonTemplate responseMock;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private Account account;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        responseMock = getMoneyPersonTemplate();
        apiSteps.createRealty3JuridicalAccount(account);
        mockRuleConfigurable.getMoneyPerson(account.getId(), responseMock.build());
        basePageSteps.resize(1400,1800);
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS_BILLING).open();
        basePageSteps.onManagementNewPage().button(ADD_PAYER).clickIf(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим ошибки")
    public void shouldSeeErrors() {
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(TITLE_NAME_SECTION).clearSign().click();
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(EMAIL_SECTION).clearSign().click();
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(PHONE_SECTION).clearSign().click();
        basePageSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(TITLE_NAME_SECTION)
                .error("Укажите название").should(isDisplayed());
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(EMAIL_SECTION)
                .error("Укажите эл. почту").should(isDisplayed());
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(PHONE_SECTION)
                .error("Укажите телефон").should(isDisplayed());
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(JURIDICAL_ADDRESS_SECTION)
                .error("Укажите юридический адрес").should(isDisplayed());
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(ADDRESS_SECTION)
                .error("Укажите почтовый адрес").should(isDisplayed());
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(INDEX_SECTION)
                .error("Укажите индекс").should(isDisplayed());
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(KPP_SECTION)
                .error("Укажите КПП").should(isDisplayed());
        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(INN_SECTION)
                .error("Укажите ИНН").should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @Ignore("ПОХОЖЕ ЧТО НЕ АКТУАЛЬНЫЙ?")
    @DisplayName("Видим реквизиты первого юрика пришедшего в моке")
    public void shouldSeeRequisites() {
        mockRuleConfigurable.getMoneyPerson(account.getId(), responseMock.build()).createWithDefaults();
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS_BILLING).open();
        basePageSteps.onManagementNewPage().button(ADD_PAYER).clickIf(isDisplayed());
        JsonObject firstJuridicalPerson = getFirstJuridicalPerson();

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(TITLE_NAME_SECTION).input()
                .should(hasValue(firstJuridicalPerson.getAsJsonPrimitive("name").getAsString()));

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(EMAIL_SECTION).input()
                .should(hasValue(firstJuridicalPerson.getAsJsonPrimitive("email").getAsString()));

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(PHONE_SECTION).input()
                .should(hasValue(firstJuridicalPerson.getAsJsonPrimitive("phone").getAsString()));

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(JURIDICAL_ADDRESS_SECTION).input()
                .should(hasValue(firstJuridicalPerson.getAsJsonPrimitive("legaladdress").getAsString()));

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(ADDRESS_SECTION).input()
                .should(hasValue(firstJuridicalPerson.getAsJsonPrimitive("postaddress").getAsString()));

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(INDEX_SECTION).input()
                .should(hasValue(firstJuridicalPerson.getAsJsonPrimitive("postcode").getAsString()));

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(INDEX_SECTION).input()
                .should(hasValue(firstJuridicalPerson.getAsJsonPrimitive("postcode").getAsString()));

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(KPP_SECTION).input()
                .should(hasValue(firstJuridicalPerson.getAsJsonPrimitive("kpp").getAsString()));

        basePageSteps.onManagementNewPage().settingsContent().sectionRequisites(INN_SECTION).input()
                .should(hasValue(firstJuridicalPerson.getAsJsonPrimitive("inn").getAsString()));
    }

    private JsonObject getFirstJuridicalPerson() {
        List<JsonObject> persons = new ArrayList<>();
        responseMock.getJson().getAsJsonObject("response").getAsJsonArray("persons")
                .forEach(p -> persons.add(p.getAsJsonObject()));
        JsonObject firstJuric = persons.stream().filter(p -> p.getAsJsonPrimitive("personType").getAsString()
                .equalsIgnoreCase("ur")).findFirst().get();
        return firstJuric;
    }
}
