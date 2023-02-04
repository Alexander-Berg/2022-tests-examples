package ru.yandex.arenda.admin.contract;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaTouchModule;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.apache.commons.lang.RandomStringUtils.randomNumeric;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.CONTRACT;
import static ru.yandex.arenda.constants.UriPath.FLAT;
import static ru.yandex.arenda.constants.UriPath.MANAGEMENT;
import static ru.yandex.arenda.constants.UriPath.MANAGER;
import static ru.yandex.arenda.matcher.AttributeMatcher.isDisabled;
import static ru.yandex.arenda.pages.ContractPage.*;
import static ru.yandex.arenda.steps.RetrofitApiSteps.ROLE_TENANT_CANDIDATE;

@Link("https://st.yandex-team.ru/VERTISTEST-1719")
@DisplayName("[Админка] Тесты на страницу договора")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaTouchModule.class)
public class ContractNewTouchPageTest {

    private String rentAmount = "45000";
    private String rentStartDate = "01062022";
    private String rentPaymentDayOfMonth = "1";
    private String contractId = getRandomString();
    private String insurancePolicyAmount = "1000";
    private String ownerName = getRandomString();
    private String ownerSurname = getRandomString();
    private String ownerPatronymic = getRandomString();
    private String ownerPhone = getRandomPhone();
    private String ownerEmail = getRandomEmail();
    private String ownerBankAccountNumber = randomNumeric(20);
    private String ownerBankAccountBik = "044525225"; // БИК сбера
    private String ownerInn = "263516479611";//какой-то инн
    private String tenantName = getRandomString();
    private String tenantSurname = getRandomString();
    private String tenantPatronymic = getRandomString();
    private String tenantPhone = getRandomPhone();
    private String tenantEmail = getRandomEmail();

    private String userIdAssigned;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private Account accountToAssign;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Before
    public void before() {
        String uid = account.getId();
        String uidAssigned = accountToAssign.getId();
        retrofitApiSteps.createUser(uid);
        retrofitApiSteps.createUser(uidAssigned);
        userIdAssigned = retrofitApiSteps.getUserId(uidAssigned);

        String createdFlatId = retrofitApiSteps.createConfirmedFlat(uid);
        retrofitApiSteps.postModerationFlatsQuestionnaire(createdFlatId);
        retrofitApiSteps.assignToUser(createdFlatId, userIdAssigned, ROLE_TENANT_CANDIDATE);
        retrofitApiSteps.okHouseService(createdFlatId);
        passportSteps.adminLogin();
        urlSteps.testing().path(MANAGEMENT).path(MANAGER).path(FLAT).path(createdFlatId).path(CONTRACT).open();
    }

    @Test
    @DisplayName("Создаем договор затем подписываем для тача")
    public void shouldAddNewContractAndSignTouch() {
        lkSteps.onContractPage().tenantSelector().click();
        lkSteps.onContractPage().stepModal().selector(TENANT_USER_ID_ID).click();
        lkSteps.onContractPage().stepModal().selectorOptionWithValue(userIdAssigned).click();
        lkSteps.onContractPage().stepModal().closeCrossModal().click();
        lkSteps.fillFieldsForCreateContractTouch(
                rentAmount,
                rentStartDate,
                rentPaymentDayOfMonth,
                ownerName,
                ownerSurname,
                ownerPatronymic,
                ownerPhone,
                ownerEmail,
                ownerBankAccountNumber,
                ownerBankAccountBik,
                ownerInn,
                tenantName,
                tenantSurname,
                tenantPatronymic,
                tenantPhone,
                tenantEmail);
        lkSteps.onContractPage().button(ADD_CONTRACT_BUTTON).click();
        lkSteps.onContractPage().successToast();
        lkSteps.onContractPage().button(CHANGE_STATUS).click();
        lkSteps.onContractPage().selector(NEW_STATUS).click();
        lkSteps.onContractPage().option("Действующий").click();
        lkSteps.onContractPage().button("Далее").click();
        lkSteps.onContractPage().button("Готово").click();
        lkSteps.onContractPage().button(SAVE_CHANGE_STATUS).click();
        lkSteps.onContractPage().signPopup().button(YES_BUTTON).click();
        lkSteps.onContractPage().successToast();
        lkSteps.onContractPage().button(CONTRACT_BUTTON).click();

        lkSteps.onContractPage().byId(RENT_AMOUNT_ID).should(isDisabled());
        lkSteps.onContractPage().byId(STATUS_ID).should(isDisabled());
        lkSteps.onContractPage().byId(RENT_START_DATE_ID).should(isDisabled());
        lkSteps.onContractPage().byId(RENT_PAYMENT_DAY_OF_MONTH_ID).should(isDisabled());
        lkSteps.onContractPage().byId(CONTRACT_ID_ID).should(isDisabled());
        lkSteps.onContractPage().byId(RENT_CALCULATED_OWNER_AMOUNT_ID).should(isDisabled());
        lkSteps.onContractPage().byId(RENT_CALCULATED_TENANT_AMOUNT_ID).should(isDisabled());

        lkSteps.onContractPage().byId(INSURANCE_POLICY_AMOUNT_ID).should(isDisabled());
        lkSteps.onContractPage().byId(INSURANCE_POLICY_DATE_ID).should(isDisabled());
        lkSteps.onContractPage().byId(INSURANCE_POLICY_ID_ID).should(isDisabled());

        lkSteps.onContractPage().byId(OWNER_NAME_ID).should(isDisabled());
        lkSteps.onContractPage().byId(OWNER_SURNAME_ID).should(isDisabled());
        lkSteps.onContractPage().byId(OWNER_PATRONYMIC_ID).should(isDisabled());
        lkSteps.onContractPage().byId(OWNER_PHONE_ID).should(isDisabled());
        lkSteps.onContractPage().byId(OWNER_EMAIL_ID).should(isDisabled());
        lkSteps.onContractPage().byId(OWNER_BANK_ACCOUNT_NUMBER_ID).should(isDisabled());
        lkSteps.onContractPage().byId(OWNER_BANK_ACCOUNT_BIK_ID).should(isDisabled());
        lkSteps.onContractPage().byId(OWNER_INN_ID).should(isDisabled());

        lkSteps.onContractPage().byId(TENANT_NAME_ID).should(isDisabled());
        lkSteps.onContractPage().byId(TENANT_SURNAME_ID).should(isDisabled());
        lkSteps.onContractPage().byId(TENANT_PATRONYMIC_ID).should(isDisabled());
        lkSteps.onContractPage().byId(TENANT_PHONE_ID).should(isDisabled());
        lkSteps.onContractPage().byId(TENANT_EMAIL_ID).should(isDisabled());
    }
}
