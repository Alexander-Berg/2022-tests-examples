package ru.yandex.payments.fnsreg;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.inject.Inject;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import ru.yandex.payments.fnsreg.controller.dto.RegReportState;
import ru.yandex.payments.fnsreg.controller.dto.RegState;
import ru.yandex.payments.fnsreg.controller.dto.WithdrawState;
import ru.yandex.payments.fnsreg.dto.ApplicationVersion;
import ru.yandex.payments.fnsreg.dto.Entity;
import ru.yandex.payments.fnsreg.dto.Report;
import ru.yandex.payments.fnsreg.error.FnModelNotSupportedException;
import ru.yandex.payments.fnsreg.error.FnsApiUnavailableException;
import ru.yandex.payments.fnsreg.error.IllegalApplicationException;
import ru.yandex.payments.fnsreg.error.KktModelNotSupportedException;
import ru.yandex.payments.fnsreg.error.RegRequestAlreadyExistsException;
import ru.yandex.payments.fnsreg.error.RegRequestNotFoundException;
import ru.yandex.payments.fnsreg.error.UnavailableActionException;
import ru.yandex.payments.fnsreg.fnsapi.DefaultFnsClient;
import ru.yandex.payments.fnsreg.fnsapi.FnsClient;
import ru.yandex.payments.fnsreg.fnsapi.MaintenanceSchedule;
import ru.yandex.payments.fnsreg.fnsapi.dto.ApplicationResponse;
import ru.yandex.payments.fnsreg.fnsapi.dto.DeviceModelInfo;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsDocumentResponse;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsError;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth.Health;
import ru.yandex.payments.fnsreg.fnsapi.dto.KktAvailableAction;
import ru.yandex.payments.fnsreg.fnsapi.dto.RegisterApplicationRequest;
import ru.yandex.payments.fnsreg.fnsapi.dto.RegistrationReportApplicationRequest;
import ru.yandex.payments.fnsreg.fnsapi.dto.RegistryRecord;
import ru.yandex.payments.fnsreg.fnsapi.dto.ShortInfo;
import ru.yandex.payments.fnsreg.fnsapi.dto.ValidateKktFnSerialRequest;
import ru.yandex.payments.fnsreg.fnsapi.dto.ValidateKktFnSerialResponse;
import ru.yandex.payments.fnsreg.fnsapi.dto.ValidateKktRegRequest;
import ru.yandex.payments.fnsreg.fnsapi.dto.ValidateKktRegResponse;
import ru.yandex.payments.fnsreg.fnsapi.dto.ValidateKktSerialRequest;
import ru.yandex.payments.fnsreg.fnsapi.dto.ValidateKktSerialResponse;
import ru.yandex.payments.fnsreg.fnsapi.dto.WithdrawApplicationRequest;
import ru.yandex.payments.fnsreg.manager.FnsHealthManager;
import ru.yandex.payments.fnsreg.manager.FnsHealthManager.Feature;
import ru.yandex.payments.fnsreg.manager.RegistrationManager;
import ru.yandex.payments.fnsreg.signapi.SignClient;
import ru.yandex.payments.fnsreg.types.Application;
import ru.yandex.payments.fnsreg.types.Inn;
import ru.yandex.payments.fnsreg.types.ModelCode;
import ru.yandex.payments.fnsreg.types.ModelName;
import ru.yandex.payments.fnsreg.types.RegRequestId;
import ru.yandex.payments.fnsreg.types.RegistrationNumber;
import ru.yandex.payments.fnsreg.types.SerialNumber;
import ru.yandex.payments.fnsreg.types.SignedApplication;
import ru.yandex.payments.fnsreg.types.Withdraw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_CLOSE_FISCAL_REPORT_FN_SIGN;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_CLOSE_FISCAL_REPORT_NUMBER;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_CLOSE_FISCAL_REPORT_TIME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_FN_MODEL_NAME;
import static ru.yandex.payments.fnsreg.TestAppGenerator.DEFAULT_KKT_MODEL_NAME;

@Property(name = "registration.test", value = "true")
@MicronautTest(propertySources = "classpath:application-reg-manager-test.yml")
class RegistrationManagerTest {
    private static final FnsHealth HEALTH_OK = new FnsHealth(
            new Health(FnsHealth.Status.AVAILABLE, "OK"),
            new Health(FnsHealth.Status.AVAILABLE, "OK"),
            new Health(FnsHealth.Status.AVAILABLE, "OK"),
            new FnsHealth.ProxyHealth(FnsHealth.ProxyStatus.AVAILABLE, "OK"),
            new Health(FnsHealth.Status.AVAILABLE, "OK")
    );

    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);
    private static final RegistrationNumber REG_NUM = new RegistrationNumber("012345");
    private static final String ENCODED_REPORT = "ZG9j"; // doc
    private static final String REJECTION_CODE = "1";
    private static final String REJECTION_REASON = "shit happens";

    private static final SerialNumber VALID_REG_KKT_SN = new SerialNumber("88888888888888888888");
    private static final SerialNumber VALID_REREG_KKT_SN = new SerialNumber("99999999999999999999");
    private static final SerialNumber VALID_REG_FN_SN = new SerialNumber("8888888888888888");
    private static final SerialNumber VALID_REREG_FN_SN = new SerialNumber("9999999999999999");
    private static final SerialNumber VALID_WITHDRAW_KKT_SN = new SerialNumber("77777777777777777777");
    private static final SerialNumber VALID_WITHDRAW_FN_SN = new SerialNumber("7777777777777777");
    private static final RegistrationNumber VALID_PREV_REG_NUM = new RegistrationNumber("5555555555555555");

    private static final SerialNumber INVALID_REG_KKT_SN = new SerialNumber("00000000111111111111");
    private static final SerialNumber INVALID_REREG_KKT_SN = new SerialNumber("00000000222222222222");
    private static final SerialNumber INVALID_REG_FN_SN = new SerialNumber("1111111111111111");
    private static final SerialNumber INVALID_REREG_FN_SN = new SerialNumber("2222222222222222");
    private static final SerialNumber INVALID_WITHDRAW_KKT_SN = new SerialNumber("33333333333333333333");
    private static final RegistrationNumber INVALID_PREV_REG_NUM = new RegistrationNumber("0003333333333333");

    private static final ModelCode MODEL_CODE = new ModelCode("1234");
    private static final ModelCode REVOKED_MODEL_CODE = new ModelCode("4321");

    @Inject
    RegistrationManager registrationManager;

    @Inject
    FnsClient fnsClientMock;

    @Inject
    FnsHealthManager fnsHealthManager;

    @Controller
    @Requires(property = "registration.test")
    public static class FnsApiController {
        private final Map<RegRequestId, RegState> regRequests = new ConcurrentHashMap<>();
        private final Map<RegRequestId, WithdrawState> withdrawRequests = new ConcurrentHashMap<>();
        private final Map<RegRequestId, RegReportState> regReportRequests = new ConcurrentHashMap<>();

        @Get("/private/tax/v1/ofds/{ofdId}/rkktapi/kkt/documents/register/{requestId}")
        public HttpResponse<Object> getRegState(@SuppressWarnings("unused") @PathVariable String ofdId,
                                                @PathVariable RegRequestId requestId) {
            val state = regRequests.get(requestId);
            if (state == null) {
                val error = new FnsError(
                        Optional.of(DefaultFnsClient.APPLICATION_BY_REQUEST_NOT_FOUND),
                        "not found",
                        Optional.empty()
                );
                return HttpResponse.unprocessableEntity()
                        .body(error);
            } else if (state instanceof RegState.InProcess) {
                final var nextState = requestId.getValue().startsWith("reject")
                        ? new RegState.Rejected(REJECTION_CODE, REJECTION_REASON)
                        : new RegState.Success(REG_NUM, ENCODED_REPORT);
                regRequests.put(requestId, nextState);
                return HttpResponse.ok(new FnsDocumentResponse.InProcess(requestId));
            } else {
                final var document = state instanceof RegState.Rejected
                        ? new FnsDocumentResponse.RejectionDocument(REJECTION_CODE, REJECTION_REASON)
                        : new FnsDocumentResponse.AcceptanceDocument(ENCODED_REPORT, Optional.of(REG_NUM));
                val result = new FnsDocumentResponse.Done(requestId, document);
                return HttpResponse.ok(result);
            }
        }

        @Get("/private/tax/v1/ofds/{ofdId}/rkktapi/kkt/documents/report/register/{requestId}")
        public HttpResponse<Object> getRegReportState(@SuppressWarnings("unused") @PathVariable String ofdId,
                                                      @PathVariable RegRequestId requestId) {
            val state = regReportRequests.get(requestId);
            if (state == null) {
                val error = new FnsError(
                        Optional.of(DefaultFnsClient.APPLICATION_BY_REQUEST_NOT_FOUND),
                        "not found",
                        Optional.empty()
                );
                return HttpResponse.unprocessableEntity()
                        .body(error);
            } else if (state instanceof RegReportState.InProcess) {
                final var nextState = requestId.getValue().startsWith("reject")
                        ? new RegReportState.Rejected(REJECTION_CODE, REJECTION_REASON)
                        : new RegReportState.Success(ENCODED_REPORT);
                regReportRequests.put(requestId, nextState);
                return HttpResponse.ok(new FnsDocumentResponse.InProcess(requestId));
            } else {
                final var document = state instanceof RegReportState.Rejected
                        ? new FnsDocumentResponse.RejectionDocument(REJECTION_CODE, REJECTION_REASON)
                        : new FnsDocumentResponse.AcceptanceDocument(ENCODED_REPORT, Optional.empty());
                val result = new FnsDocumentResponse.Done(requestId, document);
                return HttpResponse.ok(result);
            }
        }

        @Get("/private/tax/v1/ofds/{ofdId}/rkktapi/kkt/documents/unregister/{requestId}")
        public HttpResponse<Object> getWithdrawState(@SuppressWarnings("unused") @PathVariable String ofdId,
                                                     @PathVariable RegRequestId requestId) {
            val state = withdrawRequests.get(requestId);
            if (state == null) {
                val error = new FnsError(
                        Optional.of(DefaultFnsClient.APPLICATION_BY_REQUEST_NOT_FOUND),
                        "not found",
                        Optional.empty()
                );
                return HttpResponse.unprocessableEntity()
                        .body(error);
            } else if (state instanceof WithdrawState.InProcess) {
                final var nextState = requestId.getValue().startsWith("reject")
                        ? new WithdrawState.Rejected(REJECTION_CODE, REJECTION_REASON)
                        : new WithdrawState.Success(ENCODED_REPORT);
                withdrawRequests.put(requestId, nextState);
                return HttpResponse.ok(new FnsDocumentResponse.InProcess(requestId));
            } else {
                final var document = state instanceof WithdrawState.Rejected
                        ? new FnsDocumentResponse.RejectionDocument(REJECTION_CODE, REJECTION_REASON)
                        : new FnsDocumentResponse.AcceptanceDocument(ENCODED_REPORT, Optional.empty());
                val result = new FnsDocumentResponse.Done(requestId, document);
                return HttpResponse.ok(result);
            }
        }

        private static ValidateKktSerialResponse kktValidationResponse(KktAvailableAction availableAction) {
            val shortInfo = new ShortInfo("name", "1234");
            return new ValidateKktSerialResponse(availableAction, "status", shortInfo, shortInfo, Optional.empty(),
                    new Inn("12345678"), Optional.empty(), true, true, true, Optional.empty(), Optional.empty(),
                    OffsetDateTime.now(), Optional.empty(), Optional.empty(), true);
        }

        @Post("/private/tax/v1/ofds/{ofdId}/rkktapi/validations/kkt/serial")
        public ValidateKktSerialResponse validateKkt(@SuppressWarnings("unused") @PathVariable String ofdId,
                                                     @Body ValidateKktSerialRequest request) {
            if (request.serialNumber().equals(INVALID_REG_KKT_SN)) {
                return kktValidationResponse(KktAvailableAction.REREGISTRATION_OR_UNREGISTRATION);
            } else if (request.serialNumber().equals(INVALID_REREG_KKT_SN)) {
                return kktValidationResponse(KktAvailableAction.REGISTRATION);
            } else if (request.serialNumber().equals(VALID_REG_KKT_SN)) {
                return kktValidationResponse(KktAvailableAction.REGISTRATION);
            } else if (request.serialNumber().equals(VALID_REREG_KKT_SN)) {
                return kktValidationResponse(KktAvailableAction.REREGISTRATION_OR_UNREGISTRATION);
            } else if (request.serialNumber().equals(VALID_WITHDRAW_KKT_SN)) {
                return kktValidationResponse(KktAvailableAction.REREGISTRATION_OR_UNREGISTRATION);
            } else if (request.serialNumber().equals(INVALID_WITHDRAW_KKT_SN)) {
                return kktValidationResponse(KktAvailableAction.REGISTRATION);
            } else {
                return kktValidationResponse(KktAvailableAction.NOTHING);
            }
        }

        private static ValidateKktFnSerialResponse fnValidationResponse(KktAvailableAction availableAction) {
            val shortInfo = new ShortInfo("name", "1234");
            return new ValidateKktFnSerialResponse(availableAction, "status", shortInfo, shortInfo, Optional.empty(),
                    new Inn("12345678"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty());
        }

        @Post("/private/tax/v1/ofds/{ofdId}/rkktapi/validations/kkt/fn/serial")
        public ValidateKktFnSerialResponse validateFn(@SuppressWarnings("unused") @PathVariable String ofdId,
                                                      @Body ValidateKktFnSerialRequest request) {
            if (request.serialNumber().equals(INVALID_REG_FN_SN)) {
                return fnValidationResponse(KktAvailableAction.REREGISTRATION_OR_UNREGISTRATION);
            } else if (request.serialNumber().equals(INVALID_REREG_FN_SN)) {
                return fnValidationResponse(KktAvailableAction.NOTHING);
            } else if (request.serialNumber().equals(VALID_REG_FN_SN)) {
                return fnValidationResponse(KktAvailableAction.REGISTRATION);
            } else if (request.serialNumber().equals(VALID_REREG_FN_SN)) {
                return fnValidationResponse(KktAvailableAction.REGISTRATION);
            } else if (request.serialNumber().equals(VALID_WITHDRAW_FN_SN)) {
                return fnValidationResponse(KktAvailableAction.REREGISTRATION_OR_UNREGISTRATION);
            } else {
                return fnValidationResponse(KktAvailableAction.NOTHING);
            }
        }

        private static ValidateKktRegResponse prevRegValidationResponse(KktAvailableAction availableAction) {
            val modelInfo = new DeviceModelInfo("model_name", new SerialNumber("0987654321"));
            return new ValidateKktRegResponse(availableAction, "status", modelInfo, modelInfo, Optional.empty());
        }

        @Post("/private/tax/v1/ofds/{ofdId}/rkktapi/validations/kkt/regnumber")
        public ValidateKktRegResponse validateRegistration(@SuppressWarnings("unused") @PathVariable String ofdId,
                                                           @Body ValidateKktRegRequest request) {
            if (request.regNumber().equals(INVALID_PREV_REG_NUM)) {
                return prevRegValidationResponse(KktAvailableAction.REGISTRATION);
            } else if (request.regNumber().equals(VALID_PREV_REG_NUM)) {
                return prevRegValidationResponse(KktAvailableAction.REREGISTRATION_OR_UNREGISTRATION);
            } else {
                return prevRegValidationResponse(KktAvailableAction.NOTHING);
            }
        }

        @Post("/private/tax/v1/ofds/{ofdId}/rkktapi/kkt/applications/register")
        public ApplicationResponse register(@SuppressWarnings("unused") @PathVariable String ofdId,
                                            @Body RegisterApplicationRequest request) {
            assertThat(request.meta().fnModelCode())
                    .isEqualTo(MODEL_CODE);
            assertThat(request.meta().kktModelCode())
                    .isEqualTo(MODEL_CODE);

            val now = OffsetDateTime.now();
            regRequests.put(request.requestId(), RegState.IN_PROCESS);
            return new ApplicationResponse(request.requestId(), now, Optional.of(now));
        }

        @Get("/private/tax/v1/ofds/{ofdId}/rkktapi/dicts/kkt/models")
        public List<RegistryRecord> getKktModelsRegister(@SuppressWarnings("unused") @PathVariable String ofdId) {
            return List.of(
                    new RegistryRecord(MODEL_CODE, DEFAULT_KKT_MODEL_NAME, false),
                    new RegistryRecord(REVOKED_MODEL_CODE, DEFAULT_KKT_MODEL_NAME, true)
            );
        }

        @Get("/private/tax/v1/ofds/{ofdId}/rkktapi/dicts/kkt/fn/models")
        List<RegistryRecord> getFnModelsRegister(@SuppressWarnings("unused") @PathVariable String ofdId) {
            return List.of(
                    new RegistryRecord(MODEL_CODE, DEFAULT_FN_MODEL_NAME, false),
                    new RegistryRecord(REVOKED_MODEL_CODE, DEFAULT_FN_MODEL_NAME, true)
            );
        }

        @Post("/private/tax/v1/ofds/{ofdId}/rkktapi/kkt/applications/report/register")
        public ApplicationResponse regReport(@SuppressWarnings("unused") @PathVariable String ofdId,
                                             @Body RegistrationReportApplicationRequest request) {
            assertThat(request.meta().kktSerialNumber())
                    .isEqualTo(VALID_REG_KKT_SN);
            val now = OffsetDateTime.now();
            regReportRequests.put(request.requestId(), RegReportState.IN_PROCESS);
            return new ApplicationResponse(request.requestId(), now, Optional.of(now));
        }

        @Post("/private/tax/v1/ofds/{ofdId}/rkktapi/kkt/applications/unregister")
        public ApplicationResponse withdraw(@SuppressWarnings("unused") @PathVariable String ofdId,
                                            @Body WithdrawApplicationRequest request) {
            assertThat(request.meta().kktModelCode())
                    .isEqualTo(MODEL_CODE);

            val now = OffsetDateTime.now();
            withdrawRequests.put(request.requestId(), WithdrawState.IN_PROCESS);
            return new ApplicationResponse(request.requestId(), now, Optional.of(now));
        }
    }

    @MockBean(FnsHealthManager.class)
    public FnsHealthManager fnsHealthManagerMock() {
        val manager = mock(FnsHealthManager.class);
        when(manager.getHealth()).thenReturn(Mono.just(HEALTH_OK));
        when(manager.getMaintenanceSchedule()).thenReturn(Mono.just(MaintenanceSchedule.empty()));
        when(manager.checkHealth(any())).thenReturn(Mono.empty());
        return manager;
    }

    @MockBean(SignClient.class)
    public SignClient signClientMock() {
        val client = mock(SignClient.class);
        when(client.sign(any(), any()))
                .thenReturn(Mono.just(new SignedApplication("dummy signed application")));
        return client;
    }

    private static RegRequestId reqId(String value) {
        return new RegRequestId(value);
    }

    private static Application validRegistrationApplication(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(VALID_REG_KKT_SN)
                .fnSn(VALID_REG_FN_SN)
                .build()
                .toRegistrationApp();
        return new Application(appSource);
    }

    private static Application validReregistrationApplication(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(VALID_REREG_KKT_SN)
                .fnSn(VALID_REREG_FN_SN)
                .prevRegNumber(VALID_PREV_REG_NUM)
                .build()
                .toReregistrationApp();
        return new Application(appSource);
    }

    private static Application validWithdrawApplication(Withdraw withdraw) {
        val appSource = TestAppGenerator.builder()
                .kktSn(VALID_WITHDRAW_KKT_SN)
                .fnSn(VALID_WITHDRAW_FN_SN)
                .prevRegNumber(VALID_PREV_REG_NUM)
                .withdraw(withdraw)
                .build()
                .toWithdrawApp();
        return new Application(appSource);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager returns valid state for successful registration")
    void testSuccessfulRegistrationState(ApplicationVersion version) {
        val app = validRegistrationApplication(version);
        val requestId = reqId("reg-" + version);
        registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT);

        val initialState = registrationManager.getRegistrationState(requestId).block(WAIT_TIMEOUT);
        assertThat(initialState)
                .isNotNull()
                .isInstanceOf(RegState.InProcess.class);

        val nextState = registrationManager.getRegistrationState(requestId).block(WAIT_TIMEOUT);
        assertThat(nextState)
                .isNotNull()
                .isInstanceOf(RegState.Success.class)
                .isEqualTo(new RegState.Success(REG_NUM, ENCODED_REPORT));
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager returns valid state for rejected registration")
    void testRejectedRegistrationState(ApplicationVersion version) {
        val app = validRegistrationApplication(version);
        val requestId = reqId("rejected_reg-" + version);
        registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT);

        val initialState = registrationManager.getRegistrationState(requestId).block(WAIT_TIMEOUT);
        assertThat(initialState)
                .isNotNull()
                .isInstanceOf(RegState.InProcess.class);

        val nextState = registrationManager.getRegistrationState(requestId).block(WAIT_TIMEOUT);
        assertThat(nextState)
                .isNotNull()
                .isInstanceOf(RegState.Rejected.class)
                .isEqualTo(new RegState.Rejected(REJECTION_CODE, REJECTION_REASON));
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager accepts valid registration application")
    void testSuccessfulRegistration(ApplicationVersion version) {
        val app = validRegistrationApplication(version);
        val requestId = reqId("good_reg-" + version);
        assertThatCode(() -> registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager accepts valid reregistration application")
    void testSuccessfulReregistration(ApplicationVersion version) {
        val app = validReregistrationApplication(version);
        val requestId = reqId("good_rereg-" + version);
        assertThatCode(() -> registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error for invalid registration application schema")
    void testInvalidSchemaApplicationRegistration(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(VALID_REG_KKT_SN)
                .fnSn(VALID_REG_FN_SN)
                .invalidSchema(true)
                .build()
                .toRegistrationApp();

        val app = new Application(appSource);
        val requestId = reqId("bad_schema_reg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(IllegalApplicationException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error for invalid reregistration application schema")
    void testInvalidSchemaApplicationReregistration(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(VALID_REREG_KKT_SN)
                .fnSn(VALID_REREG_FN_SN)
                .prevRegNumber(VALID_PREV_REG_NUM)
                .invalidSchema(true)
                .build()
                .toReregistrationApp();

        val app = new Application(appSource);
        val requestId = reqId("bad_schema_rereg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(IllegalApplicationException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error for invalid kkt registration")
    void testInvalidKktApplicationRegistration(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(INVALID_REG_KKT_SN)
                .fnSn(VALID_REG_FN_SN)
                .build()
                .toRegistrationApp();

        val app = new Application(appSource);
        val requestId = reqId("bad_kkt_reg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(UnavailableActionException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error for invalid kkt reregistration")
    void testInvalidKktApplicationReregistration(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(INVALID_REREG_KKT_SN)
                .fnSn(VALID_REREG_FN_SN)
                .prevRegNumber(VALID_PREV_REG_NUM)
                .build()
                .toReregistrationApp();

        val app = new Application(appSource);
        val requestId = reqId("bad_kkt_rereg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(UnavailableActionException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error for invalid fn registration")
    void testInvalidFnApplicationRegistration(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(VALID_REG_KKT_SN)
                .fnSn(INVALID_REG_FN_SN)
                .build()
                .toRegistrationApp();

        val app = new Application(appSource);
        val requestId = reqId("bad_fn_reg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(UnavailableActionException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error for invalid fn reregistration")
    void testInvalidFnApplicationReregistration(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(VALID_REREG_KKT_SN)
                .fnSn(INVALID_REREG_FN_SN)
                .prevRegNumber(VALID_PREV_REG_NUM)
                .build()
                .toReregistrationApp();

        val app = new Application(appSource);
        val requestId = reqId("bad_fn_rereg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(UnavailableActionException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error for reregistration with invalid prev registration")
    void testInvalidPrevRegistrationApplicationReregistration(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(VALID_REREG_KKT_SN)
                .fnSn(VALID_REREG_FN_SN)
                .prevRegNumber(INVALID_PREV_REG_NUM)
                .build()
                .toReregistrationApp();

        val app = new Application(appSource);
        val requestId = reqId("bad_prev_reg_rereg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(UnavailableActionException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error if fns registration api is unavailable")
    void testUnavailableRegistrationApi(ApplicationVersion version) {
        when(fnsHealthManager.checkHealth(eq(Feature.REGISTRATION)))
                .thenThrow(new FnsApiUnavailableException(Duration.ZERO, Optional.empty(), null));

        val app = validRegistrationApplication(version);
        val requestId = reqId("no_api_reg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(FnsApiUnavailableException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error if fns reregistration api is unavailable")
    void testUnavailableReregistrationApi(ApplicationVersion version) {
        when(fnsHealthManager.checkHealth(eq(Feature.REGISTRATION)))
                .thenThrow(new FnsApiUnavailableException(Duration.ZERO, Optional.empty(), null));

        val app = validReregistrationApplication(version);
        val requestId = reqId("no_api_rereg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(FnsApiUnavailableException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error if user is trying to create registration using " +
            "existing request id")
    void testRegUsingExistingRequestId(ApplicationVersion version) {
        val app = validRegistrationApplication(version);
        val requestId = reqId("dup_reg-" + version);

        assertThatCode(() -> registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT))
                .isInstanceOf(RegRequestAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Verify that registration manager throws an error if user trying to fetch registration state by" +
            "unknown request id")
    void testGetUnknownRegRequestState() {
        assertThatThrownBy(() -> registrationManager.getRegistrationState(reqId("unknown")).block(WAIT_TIMEOUT))
                .isInstanceOf(RegRequestNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error while registering an unknown kkt model")
    void testUnknownKktModelReg(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(VALID_REG_KKT_SN)
                .fnSn(VALID_REG_FN_SN)
                .kktModelName(new ModelName("РП Система неизвестность"))
                .build()
                .toRegistrationApp();

        val app = new Application(appSource);
        val requestId = reqId("unknown_kkt_reg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(KktModelNotSupportedException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that registration manager throws an error while registering an unknown fn model")
    void testUnknownFnModel(ApplicationVersion version) {
        val appSource = TestAppGenerator.builder()
                .appVersion(version)
                .kktSn(VALID_REG_KKT_SN)
                .fnSn(VALID_REG_FN_SN)
                .fnModelName(new ModelName("«ФН-1.1» неизвестное исполнение"))
                .build()
                .toRegistrationApp();

        val app = new Application(appSource);
        val requestId = reqId("unknown_fn_reg-" + version);

        assertThatThrownBy(() ->
                registrationManager.createRegistration(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(FnModelNotSupportedException.class);
    }

    @ParameterizedTest
    @EnumSource(ApplicationVersion.class)
    @DisplayName("Verify that RegistrationManager doesn't check KKT or FN if validation is disabled")
    void testNoValidation(ApplicationVersion version) {
        assertThatCode(() -> {
            val appSource = TestAppGenerator.builder()
                    .appVersion(version)
                    .kktSn(INVALID_REREG_KKT_SN)
                    .fnSn(VALID_REREG_FN_SN)
                    .prevRegNumber(VALID_PREV_REG_NUM)
                    .build()
                    .toReregistrationApp();

            val app = new Application(appSource);
            val noValidation = EnumSet.of(Entity.KKT);
            val requestId = reqId("no_kkt_check_reg-" + version);

            registrationManager.createRegistration(requestId, app, noValidation).block(WAIT_TIMEOUT);
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            val appSource = TestAppGenerator.builder()
                    .appVersion(version)
                    .kktSn(VALID_REREG_KKT_SN)
                    .fnSn(INVALID_REREG_FN_SN)
                    .prevRegNumber(VALID_PREV_REG_NUM)
                    .build()
                    .toReregistrationApp();

            val app = new Application(appSource);
            val noValidation = EnumSet.of(Entity.FN);
            val requestId = reqId("no_fn_check_reg-" + version);

            registrationManager.createRegistration(requestId, app, noValidation).block(WAIT_TIMEOUT);
        }).doesNotThrowAnyException();
    }

    // ---- withdraw

    static Stream<Arguments> withdrawArgs() {
        return Stream.of(
                Arguments.of(Withdraw.KKT_STOLEN),
                Arguments.of(Withdraw.KKT_MISSING),
                Arguments.of(Withdraw.FN_BROKEN),
                Arguments.of(new Withdraw.FiscalClose(
                                new Report(
                                        DEFAULT_CLOSE_FISCAL_REPORT_TIME,
                                        DEFAULT_CLOSE_FISCAL_REPORT_NUMBER,
                                        DEFAULT_CLOSE_FISCAL_REPORT_FN_SIGN
                                )
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("withdrawArgs")
    @DisplayName("Verify that registration manager returns valid state for successful withdraw")
    void testSuccessfulWithdrawState(Withdraw withdraw) {
        val app = validWithdrawApplication(withdraw);
        val requestId = reqId("withdraw-" + withdraw.reason());
        registrationManager.createWithdraw(requestId, app).block(WAIT_TIMEOUT);

        val initialState = registrationManager.getWithdrawState(requestId).block(WAIT_TIMEOUT);
        assertThat(initialState)
                .isNotNull()
                .isInstanceOf(WithdrawState.InProcess.class);

        val nextState = registrationManager.getWithdrawState(requestId).block(WAIT_TIMEOUT);
        assertThat(nextState)
                .isNotNull()
                .isInstanceOf(WithdrawState.Success.class)
                .isEqualTo(new WithdrawState.Success(ENCODED_REPORT));
    }

    @ParameterizedTest
    @MethodSource("withdrawArgs")
    @DisplayName("Verify that registration manager returns valid state for rejected withdraw")
    void testRejectedWithdrawState(Withdraw withdraw) {
        val app = validWithdrawApplication(withdraw);
        val requestId = reqId("rejected_withdraw-" + withdraw.reason());
        registrationManager.createWithdraw(requestId, app).block(WAIT_TIMEOUT);

        val initialState = registrationManager.getWithdrawState(requestId).block(WAIT_TIMEOUT);
        assertThat(initialState)
                .isNotNull()
                .isInstanceOf(WithdrawState.InProcess.class);

        val nextState = registrationManager.getWithdrawState(requestId).block(WAIT_TIMEOUT);
        assertThat(nextState)
                .isNotNull()
                .isInstanceOf(WithdrawState.Rejected.class)
                .isEqualTo(new WithdrawState.Rejected(REJECTION_CODE, REJECTION_REASON));
    }

    @ParameterizedTest
    @MethodSource("withdrawArgs")
    @DisplayName("Verify that registration manager accepts valid withdraw application")
    void testSuccessfulWithdraw(Withdraw withdraw) {
        val app = validWithdrawApplication(withdraw);
        val requestId = reqId("good_withdraw-" + withdraw.reason());
        assertThatCode(() -> registrationManager.createWithdraw(requestId, app).block(WAIT_TIMEOUT))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("withdrawArgs")
    @DisplayName("Verify that registration manager throws an error for invalid kkt withdraw")
    void testInvalidKktApplicationWithdraw(Withdraw withdraw) {
        val appSource = TestAppGenerator.builder()
                .kktSn(INVALID_WITHDRAW_KKT_SN)
                .fnSn(VALID_WITHDRAW_FN_SN)
                .prevRegNumber(VALID_PREV_REG_NUM)
                .withdraw(withdraw)
                .build()
                .toWithdrawApp();

        val app = new Application(appSource);
        val requestId = reqId("bad_kkt_withdraw-" + withdraw.reason());
        assertThatThrownBy(() ->
                registrationManager.createWithdraw(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(UnavailableActionException.class);
    }

    @ParameterizedTest
    @MethodSource("withdrawArgs")
    @DisplayName("Verify that registration manager throws an error if fns withdraw api is unavailable")
    void testUnavailableWithdrawApi(Withdraw withdraw) {
        when(fnsHealthManager.checkHealth(eq(Feature.REGISTRATION)))
                .thenThrow(new FnsApiUnavailableException(Duration.ZERO, Optional.empty(), null));

        val app = validWithdrawApplication(withdraw);
        val requestId = reqId("no_api_withdraw-" + withdraw.reason());
        assertThatThrownBy(() ->
                registrationManager.createWithdraw(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(FnsApiUnavailableException.class);
    }

    @ParameterizedTest
    @MethodSource("withdrawArgs")
    @DisplayName("Verify that registration manager throws an error if user is trying to create withdraw using " +
            "existing request id")
    void testWithdrawUsingExistingRequestId(Withdraw withdraw) {
        val app = validWithdrawApplication(withdraw);
        val requestId = reqId("dup_withdraw-" + withdraw.reason());

        assertThatCode(() -> registrationManager.createWithdraw(requestId, app).block(WAIT_TIMEOUT))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> registrationManager.createWithdraw(requestId, app).block(WAIT_TIMEOUT))
                .isInstanceOf(RegRequestAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Verify that registration manager throws an error if user trying to fetch withdraw state by" +
            "unknown request id")
    void testGetUnknownWithdrawRequestState() {
        assertThatThrownBy(() -> registrationManager.getWithdrawState(reqId("unknown-withdraw")).block(WAIT_TIMEOUT))
                .isInstanceOf(RegRequestNotFoundException.class);
    }

    @ParameterizedTest
    @MethodSource("withdrawArgs")
    @DisplayName("Verify that registration manager throws an error while withdrawing an unknown kkt model")
    void testUnknownKktModelWithdraw(Withdraw withdraw) {
        val appSource = TestAppGenerator.builder()
                .kktSn(VALID_WITHDRAW_KKT_SN)
                .fnSn(VALID_WITHDRAW_FN_SN)
                .prevRegNumber(VALID_PREV_REG_NUM)
                .kktModelName(new ModelName("РП Система неизвестность"))
                .withdraw(withdraw)
                .build()
                .toWithdrawApp();

        val app = new Application(appSource);
        val requestId = reqId("unknown_kkt_withdraw-" + withdraw.reason());

        assertThatThrownBy(() ->
                registrationManager.createWithdraw(requestId, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(KktModelNotSupportedException.class);
    }

    private static Application validRegReportApplication() {
        val appSource = TestAppGenerator.builder()
                .kktSn(VALID_REG_KKT_SN)
                .fnSn(VALID_REG_FN_SN)
                .build()
                .toRegReportApp();
        return new Application(appSource);
    }

    @Test
    @DisplayName("Verify that registration manager accepts valid registration report application")
    void testSuccessfulRegistrationReport() {
        val app = validRegReportApplication();
        val requestId = reqId("good_reg_report");

        assertThatCode(() -> registrationManager.createRegistrationReport(requestId, VALID_REG_KKT_SN, DEFAULT_KKT_MODEL_NAME, app).block(WAIT_TIMEOUT))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Verify that registration manager throws error in case of submitting reg report with existing id")
    void testRegReportUsingExistingRequestId() {
        val app = validRegReportApplication();
        val requestId = reqId("duplicate_reg_report");

        assertThatCode(() -> registrationManager.createRegistrationReport(requestId, VALID_REG_KKT_SN, DEFAULT_KKT_MODEL_NAME, app).block(WAIT_TIMEOUT))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> registrationManager.createRegistrationReport(requestId, VALID_REG_KKT_SN, DEFAULT_KKT_MODEL_NAME, app).block(WAIT_TIMEOUT))
                .isInstanceOf(RegRequestAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Verify that registration manager throws an error if fns reg report api is unavailable")
    void testUnavailableRegReportApi() {
        when(fnsHealthManager.checkHealth(eq(Feature.REGISTRATION)))
                .thenThrow(new FnsApiUnavailableException(Duration.ZERO, Optional.empty(), null));

        val app = validRegReportApplication();
        val requestId = reqId("no_api_reg_report");

        assertThatThrownBy(() ->
                registrationManager.createRegistrationReport(requestId, VALID_REG_KKT_SN, DEFAULT_KKT_MODEL_NAME, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(FnsApiUnavailableException.class);
    }

    @Test
    @DisplayName("Verify that registration manager throws an error if user trying to fetch reg report state by" +
            "unknown request id")
    void testGetUnknownRegReportRequestState() {
        assertThatThrownBy(() -> registrationManager.getRegReportState(reqId("unknown-reg-report")).block(WAIT_TIMEOUT))
                .isInstanceOf(RegRequestNotFoundException.class);
    }

    @Test
    @DisplayName("Verify that registration manager throws an error for invalid kkt reg report")
    void testInvalidKktApplicationReportReg() {
        val appSource = TestAppGenerator.builder()
                .fnSn(VALID_REG_FN_SN)
                .build()
                .toRegReportApp();
        val app = new Application(appSource);
        val requestId = reqId("bad_kkt_reg_report");
        assertThatThrownBy(() ->
                registrationManager.createRegistrationReport(requestId, INVALID_REG_KKT_SN, DEFAULT_KKT_MODEL_NAME, app).block(WAIT_TIMEOUT)
        ).isInstanceOf(UnavailableActionException.class);
    }
}
