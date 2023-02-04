package ru.yandex.payments.fnsreg;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

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
import reactor.core.publisher.Mono;

import ru.yandex.payments.fnsreg.error.FiscalStorageSerialNumberNotFoundException;
import ru.yandex.payments.fnsreg.error.KktRegNumberNotFoundException;
import ru.yandex.payments.fnsreg.error.KktSerialNumberNotFoundException;
import ru.yandex.payments.fnsreg.fnsapi.DefaultFnsClient;
import ru.yandex.payments.fnsreg.fnsapi.MaintenanceSchedule;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsError;
import ru.yandex.payments.fnsreg.fnsapi.dto.FnsHealth;
import ru.yandex.payments.fnsreg.fnsapi.dto.RegistryRecord;
import ru.yandex.payments.fnsreg.fnsapi.dto.ValidateKktFnSerialRequest;
import ru.yandex.payments.fnsreg.fnsapi.dto.ValidateKktRegRequest;
import ru.yandex.payments.fnsreg.fnsapi.dto.ValidateKktSerialRequest;
import ru.yandex.payments.fnsreg.manager.DeviceManager;
import ru.yandex.payments.fnsreg.manager.FnsHealthManager;
import ru.yandex.payments.fnsreg.manager.RegistrationManager;
import ru.yandex.payments.fnsreg.signapi.SignClient;
import ru.yandex.payments.fnsreg.types.Inn;
import ru.yandex.payments.fnsreg.types.ModelCode;
import ru.yandex.payments.fnsreg.types.ModelName;
import ru.yandex.payments.fnsreg.types.RegistrationNumber;
import ru.yandex.payments.fnsreg.types.SerialNumber;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Property(name = "diagnostic.test", value = "true")
@MicronautTest(propertySources = "classpath:application-diagnostics-test.yml")
public class DiagnosticHandlesTest {
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(10);
    private static final ModelName KKT_MODEL_NAME = new ModelName("РП Система 1ФА");
    private static final ModelCode MODEL_CODE = new ModelCode("1234");

    @Inject
    DeviceManager deviceManager;
    @Inject
    RegistrationManager registrationManager;

    @Controller
    @Requires(property = "diagnostic.test")
    public static class FnsApiController {
        private static HttpResponse<Object> getError(String code) {
            val error = new FnsError(
                    Optional.of(code),
                    "not found",
                    Optional.empty()
            );
            return HttpResponse.unprocessableEntity()
                    .body(error);
        }

        @Post("/private/tax/v1/ofds/{ofdId}/rkktapi/validations/kkt/fn/serial")
        @SuppressWarnings("unused")
        public HttpResponse<Object> validateFn(@PathVariable @NotBlank String ofdId,
                                               @Valid @Body ValidateKktFnSerialRequest request) {
            return getError(DefaultFnsClient.FS_BY_SERIALNUMBER_NOT_FOUND);
        }

        @Post("/private/tax/v1/ofds/{ofdId}/rkktapi/validations/kkt/serial")
        @SuppressWarnings("unused")
        public HttpResponse<Object> validateKkt(@PathVariable String ofdId,
                                                @Body ValidateKktSerialRequest request) {
            return getError(DefaultFnsClient.KKT_BY_SERIALNUMBER_AND_MODELCODE_NOT_FOUND);
        }

        @Post("/private/tax/v1/ofds/{ofdId}/rkktapi/validations/kkt/regnumber")
        @SuppressWarnings("unused")
        public HttpResponse<Object> validateRegistration(@PathVariable String ofdId,
                                                         @Body ValidateKktRegRequest request) {
            return getError(DefaultFnsClient.KKT_BY_REGNUMBER_NOT_FOUND);
        }

        @Get("/private/tax/v1/ofds/{ofdId}/rkktapi/dicts/kkt/models")
        public List<RegistryRecord> getKktModelsRegister(@SuppressWarnings("unused") @PathVariable String ofdId) {
            return List.of(
                    new RegistryRecord(MODEL_CODE, KKT_MODEL_NAME, false)
            );
        }
    }

    private static final FnsHealth HEALTH_OK = new FnsHealth(
            new FnsHealth.Health(FnsHealth.Status.AVAILABLE, "OK"),
            new FnsHealth.Health(FnsHealth.Status.AVAILABLE, "OK"),
            new FnsHealth.Health(FnsHealth.Status.AVAILABLE, "OK"),
            new FnsHealth.ProxyHealth(FnsHealth.ProxyStatus.AVAILABLE, "OK"),
            new FnsHealth.Health(FnsHealth.Status.AVAILABLE, "OK")
    );

    @MockBean(SignClient.class)
    public SignClient signClientMock() {
        return mock(SignClient.class);
    }

    @MockBean(FnsHealthManager.class)
    public FnsHealthManager fnsHealthManagerMock() {
        val manager = mock(FnsHealthManager.class);
        when(manager.getHealth()).thenReturn(Mono.just(HEALTH_OK));
        when(manager.getMaintenanceSchedule()).thenReturn(Mono.just(MaintenanceSchedule.empty()));
        when(manager.checkHealth(any())).thenReturn(Mono.empty());
        return manager;
    }

    @Test
    @DisplayName("Check that fiscal storage returns 404 exception")
    public void testFiscalStorageNotFound() {
        assertThatThrownBy(() -> deviceManager.getFiscalStorageFull(new SerialNumber("12345")).block(BLOCK_TIMEOUT))
                .isInstanceOf(FiscalStorageSerialNumberNotFoundException.class);
    }

    @Test
    @DisplayName("Check that kkt lookup by serial number returns 404 exception")
    public void testKktSerialNumberNotFound() {
        assertThatThrownBy(() ->
                deviceManager.getCashRegisterFull(new SerialNumber("12345"), KKT_MODEL_NAME).block(BLOCK_TIMEOUT))
                .isInstanceOf(KktSerialNumberNotFoundException.class);
    }

    @Test
    @DisplayName("Check that kkt lookup by reg number returns 404 exception")
    public void testKktRegNumberNotFound() {
        val regNumber = new RegistrationNumber("12345");
        val inn = new Inn("12345");
        assertThatThrownBy(() -> registrationManager.getRegistrationState(regNumber, inn).block(BLOCK_TIMEOUT))
                .isInstanceOf(KktRegNumberNotFoundException.class);
    }
}
