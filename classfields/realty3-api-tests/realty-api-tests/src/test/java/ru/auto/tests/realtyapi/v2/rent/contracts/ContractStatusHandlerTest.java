package ru.auto.tests.realtyapi.v2.rent.contracts;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import lombok.extern.log4j.Log4j;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.RealtyTestApiModule;
import ru.auto.tests.realtyapi.utils.UtilsRealtyApi;
import ru.auto.tests.realtyapi.v1.ResponseSpecBuilders;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiUpdateContractStatusError;
import ru.auto.tests.realtyapi.v2.model.RealtyPersonFullName;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlat;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlatShowing;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiPostModerationContractRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentContract;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentContractOwnerInfo;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentContractTenantInfo;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentUser;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiUpdateContractStatusError;
import ru.auto.tests.realtyapi.v2.rent.AbstractHandlerTest;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.UUID;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;

@Title("PUT /rent/moderation/flats/{flatId}/contracts/{contractId}/update-status")
@RunWith(GuiceTestRunner.class)
@Log4j
@GuiceModules(RealtyTestApiModule.class)
public class ContractStatusHandlerTest extends AbstractHandlerTest {

    private RealtyRentApiFlat flat;
    private RealtyRentApiRentUser tenant;

    @Inject
    private ApiClient apiV2;

    @Before
    public void before() {
        createAccounts();
        createUser("Owner", token1, uid1);
        tenant = createTenant();
        flat = createActualFlat();
        createFlatQuestionnaire();
        createFlatShowing();
    }


    @AfterClass
    public static void cleaning() {
        deleteDrafts(token1);
        deleteUsers(token1);
        deleteAccounts();
    }

    @Test
    @DisplayName("Попытка привязать страховку после активации договора")
    public void duplicateInsurancePolicyTest() {
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(
                rentApiAdaptor.buildOwnerInfo(), rentApiAdaptor.buildTenantInfo(tenant.getUserId()));
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());
        rentApiAdaptor.sendContractToOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByTenant(token1, flat.getFlatId(), contract.getContractId());
        apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildContractActivateRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200Ok()));

        RealtyApiUpdateContractStatusError response = apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildInsurancePolicyRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBeCode(400)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка о дублировании полисов")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки о дублировании полисов")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.UPDATE_RENT_CONTRACT_STATUS_ERROR);
        Assertions
                .assertThat(response.getData())
                .describedAs("Внутренняя ошибка о дублировании полисов")
                .isNotNull();
        Assertions
                .assertThat(response.getData().getErrorCode())
                .describedAs("Код внутренней ошибки о дублировании полисов")
                .isEqualTo(RealtyRentApiUpdateContractStatusError.ErrorCodeEnum.SET_INSURANCE_ALREADY_ADDED_ERROR);
    }

    @Test
    @DisplayName("Не существующий договор")
    public void notFoundContractTest() {
        RealtyApiUpdateContractStatusError response = apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(UUID.randomUUID())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildInsurancePolicyRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBeCode(404)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка об отсутствующем контракте")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки об отсутствующем контракте")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.NOT_FOUND);
//        Assertions
//                .assertThat(response.getUpdateStatusError())
//                .describedAs("Внутренняя ошибка")
//                .isNull();
    }

    @Test
    @DisplayName("Не существующая квартира")
    public void notFoundFlatTest() {
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(tenant.getUserId());
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());
        rentApiAdaptor.sendContractToOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByTenant(token1, flat.getFlatId(), contract.getContractId());

        RealtyApiUpdateContractStatusError response = apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(UUID.randomUUID())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildInsurancePolicyRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBeCode(404)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка об отсутствующей квартире")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки об отсутствующей квартире")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.NOT_FOUND);
//        Assertions
//                .assertThat(response.getUpdateStatusError())
//                .describedAs("Внутренняя ошибка")
//                .isNull();
    }

    @Test
    @DisplayName("Неактивный договор")
    public void notActivateContract() {
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(tenant.getUserId());
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
//        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
//        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());
        RealtyApiUpdateContractStatusError response = apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildInsurancePolicyRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBeCode(400)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка о не активированном договоре")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки о не активированном договоре")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.UPDATE_RENT_CONTRACT_STATUS_ERROR);
//        Assertions
//                .assertThat(response.getUpdateStatusError())
//                .describedAs("Внутренняя ошибка о не активированном договоре")
//                .isNotNull();
//        Assertions
//                .assertThat(response.getUpdateStatusError().getErrorCode())
//                .describedAs("Код внутренней ошибки о не активированном договоре")
//                .isEqualTo(RealtyRentApiUpdateContractStatusError.ErrorCodeEnum.SET_INSURANCE_CONTRACT_NOT_ACTIVE_ERROR);
    }

    @Test
    @DisplayName("Отсутствие имени у собственника")
    public void emptyOwnerName() {
        RealtyRentApiRentContractOwnerInfo ownerInfo = rentApiAdaptor.buildOwnerInfo()
                .person(new RealtyPersonFullName());
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(
                ownerInfo, rentApiAdaptor.buildTenantInfo(tenant.getUserId()));
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());
        rentApiAdaptor.sendContractToOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByTenant(token1, flat.getFlatId(), contract.getContractId());

        apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildContractActivateRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200Ok()));
        RealtyApiUpdateContractStatusError response = apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildInsurancePolicyRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBeCode(400)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка о пустом имени собственника")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки о пустом имени собственника")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.UPDATE_RENT_CONTRACT_STATUS_ERROR);
//        Assertions
//                .assertThat(response.getUpdateStatusError())
//                .describedAs("Внутренняя ошибка о пустом имени собственника")
//                .isNotNull();
//        Assertions
//                .assertThat(response.getUpdateStatusError().getErrorCode())
//                .describedAs("Код внутренней ошибки о пустом имени собственника")
//                .isEqualTo(RealtyRentApiUpdateContractStatusError.ErrorCodeEnum.SET_INSURANCE_EMPTY_OWNER_NAME_ERROR);
    }

    @Test
    @DisplayName("Отсутствие почты у собственника")
    public void emptyOwnerEmail() {
        RealtyRentApiRentContractOwnerInfo ownerInfo = rentApiAdaptor.buildOwnerInfo()
                .email(null);
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(
                ownerInfo, rentApiAdaptor.buildTenantInfo(tenant.getUserId()));
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());
        RealtyApiUpdateContractStatusError response = apiV2.rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec())
                .authorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildSendContractToOwnerRequest())
                .executeAs(validatedWith(shouldBeCode(400)))
                .getError();
        Assertions
                .assertThat(response)
                .describedAs("Ошибка о пустой почте собственника")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки о пустой почте собственника")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.INVALID_PARAMS);
    }

    @Test
    @DisplayName("Отсутствие телефона у собственника")
    public void emptyOwnerPhone() {
        RealtyRentApiRentContractOwnerInfo ownerInfo = rentApiAdaptor.buildOwnerInfo()
                .phone(null);
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(
                ownerInfo, rentApiAdaptor.buildTenantInfo(tenant.getUserId()));
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());

        RealtyApiUpdateContractStatusError response = apiV2.rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec())
                .authorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildSendContractToOwnerRequest())
                .executeAs(validatedWith(shouldBeCode(400)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка о пустом телефоне собственника")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки о пустом телефоне собственника")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.INVALID_PARAMS);
    }

    @Test
    @DisplayName("Отсутствие имени у жильца")
    public void emptyTenantName() {
        RealtyRentApiRentContractTenantInfo tenantInfo = rentApiAdaptor.buildTenantInfo(tenant.getUserId())
                .person(new RealtyPersonFullName());
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(
                rentApiAdaptor.buildOwnerInfo(), tenantInfo);
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());
        rentApiAdaptor.sendContractToOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByTenant(token1, flat.getFlatId(), contract.getContractId());

        apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildContractActivateRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200Ok()));
        RealtyApiUpdateContractStatusError response = apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildInsurancePolicyRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBeCode(400)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка о пустом имени жильца")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки о пустом имени жильца")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.UPDATE_RENT_CONTRACT_STATUS_ERROR);
//        Assertions
//                .assertThat(response.getUpdateStatusError())
//                .describedAs("Внутренняя ошибка о пустом имени жильца")
//                .isNotNull();
//        Assertions
//                .assertThat(response.getUpdateStatusError().getErrorCode())
//                .describedAs("Код внутренней ошибки о пустом имени жильца")
//                .isEqualTo(RealtyRentApiUpdateContractStatusError.ErrorCodeEnum.SET_INSURANCE_EMPTY_TENANT_NAME_ERROR);
    }

    @Test
    @DisplayName("Отсутствие почты у жильца")
    public void emptyTenantEmail() {
        RealtyRentApiRentContractTenantInfo tenantInfo = rentApiAdaptor.buildTenantInfo(tenant.getUserId())
                .email(null);
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(
                rentApiAdaptor.buildOwnerInfo(), tenantInfo);
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());

        RealtyApiUpdateContractStatusError response = apiV2.rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec())
                .authorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildSendContractToOwnerRequest())
                .executeAs(validatedWith(shouldBeCode(400)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка о пустой почте жильца")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки о пустой почте жильца")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.INVALID_PARAMS);
    }

    @Test
    @DisplayName("Отсутствие телефона у жильца")
    public void emptyTenantPhone() {
        RealtyRentApiRentContractTenantInfo tenantInfo = rentApiAdaptor.buildTenantInfo(tenant.getUserId())
                .phone(null);
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(
                rentApiAdaptor.buildOwnerInfo(), tenantInfo);
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());

        RealtyApiUpdateContractStatusError response = apiV2.rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec())
                .authorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildSendContractToOwnerRequest())
                .executeAs(validatedWith(shouldBeCode(400)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка о пустом телефоне жильца")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки о пустом телефоне жильца")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.INVALID_PARAMS);
    }

    @Test
    @DisplayName("Нулевая сумма страховки")
    public void zeroInsuranceAmount() {
        RealtyRentApiPostModerationContractRequest request = rentApiAdaptor.buildContract(
                rentApiAdaptor.buildOwnerInfo(), rentApiAdaptor.buildTenantInfo(tenant.getUserId()));
        RealtyRentApiRentContract contract = rentApiAdaptor.createContract(token1, flat.getFlatId(), request)
                .getContract();
        rentApiAdaptor.fillHouseServiceSettingsByOwner(token1, uid1, flat.getFlatId());
        rentApiAdaptor.confirmHouseServiceSettingsByTenant(token2, uid2, flat.getFlatId());
        rentApiAdaptor.sendContractToOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByOwner(token1, flat.getFlatId(), contract.getContractId());
        rentApiAdaptor.signContractByTenant(token1, flat.getFlatId(), contract.getContractId());

        apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildContractActivateRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200Ok()));
        RealtyApiUpdateContractStatusError response = apiV2
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flat.getFlatId())
                .contractIdPath(contract.getContractId())
                .reqSpec(authSpec()).xAuthorizationHeader(token1)
                .xUidHeader(token1)
                .body(rentApiAdaptor.buildInsurancePolicyRequest())
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBeCode(400)))
                .getError();

        Assertions
                .assertThat(response)
                .describedAs("Ошибка о нулевой сумме страховки")
                .isNotNull();
        Assertions
                .assertThat(response.getCode())
                .describedAs("Код ошибки о нулевой сумме страховки")
                .isEqualTo(RealtyApiUpdateContractStatusError.CodeEnum.UPDATE_RENT_CONTRACT_STATUS_ERROR);
//        Assertions
//                .assertThat(response.getUpdateStatusError())
//                .describedAs("Внутренняя ошибка о нулевой сумме страховки")
//                .isNotNull();
//        Assertions
//                .assertThat(response.getUpdateStatusError().getErrorCode())
//                .describedAs("Код внутренней ошибки о нулевой сумме страховки")
//                .isEqualTo(RealtyRentApiUpdateContractStatusError.ErrorCodeEnum.SET_INSURANCE_ZERO_INSURANCE_ERROR);
    }

    private RealtyRentApiRentUser createUser(String role, String token, String uid) {
        log.info("Create " + role + " user");
        try {
            usersToDelete.add(rentApiAdaptor.getOrCreateRentUser(token, uid));
            RealtyRentApiRentUser user = rentApiAdaptor.createRentPalmaUser(token, uid);
            log.info(role + " user has been created " + user.getUserId());
            return user;
        } catch (Throwable e) {
            log.info("Can't create " + role + " user", e);
        }
        return null;
    }

    private RealtyRentApiRentUser createTenant() {
        RealtyRentApiRentUser user = createUser("Tenant", token2, uid2);
        try {
            rentApiAdaptor.updateUserPhone(token2, uid2, UtilsRealtyApi.getAutoTestsPhone());
        } catch (Throwable ignore) {}
        return user;
    }

    private RealtyRentApiFlat createActualFlat() {
        log.info("Create actual flat draft and assign tenant");
        try {
            RealtyRentApiFlat flat = rentApiAdaptor.createFlatDraft(token1, uid1).getFlat();
            flatToDelete.add(flat);
            rentApiAdaptor.sendSmsConfirmation(token1, uid1);
            rentApiAdaptor.assignTenantCandidateToFlat(token1, flat.getFlatId(), tenant.getUserId());
            log.info("Flat has been created " + flat.getFlatId());
            return flat;
        } catch (Throwable e) {
            log.info("Can't create flat", e);
            return null;
        }
    }

    private void createFlatQuestionnaire() {
        log.info("Create flat questionnaire for flat " + flat.getFlatId());
        try {
            rentApiAdaptor.createFlatQuestionnaire(token1, flat.getFlatId());
            log.info("Flat questionnaire for flat " + flat.getFlatId() + " has been created");
        } catch (Throwable e) {
            log.info("Can't create flat questionnaire for flat " + flat.getFlatId());
        }
    }

    /**
     * Без показа с утвержденным составом жилец не сможет принять условия ЖКХ
     */
    private void createFlatShowing() {
        try {
            log.info("Create flat showing for flat " + flat.getFlatId());
            RealtyRentApiFlatShowing flatShowing = rentApiAdaptor.createFlatShowing(token1, flat.getFlatId(), UtilsRealtyApi.getAutoTestsPhone());
            log.info("Flat showing " + flatShowing.getShowingId() + " has been created");
            // Перевод в этот статус автоматический утверждает группу жильцов
            rentApiAdaptor.setFlatShowingStatusToSigningAppointed(token1, flat.getFlatId(), flatShowing.getShowingId());
            log.info("Flat showing " + flatShowing.getShowingId() + " status has been changed");
        } catch (Throwable ignored) {}
    }
}
