package ru.yandex.intranet.d.services.operations

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestProviders
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel
import ru.yandex.intranet.d.model.accounts.OperationChangesModel
import ru.yandex.intranet.d.model.accounts.OperationErrorKind
import ru.yandex.intranet.d.model.accounts.OperationOrdersModel
import ru.yandex.intranet.d.model.accounts.OperationSource
import ru.yandex.intranet.d.services.integration.jns.JnsClientStub
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Operations observability service test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class OperationsObservabilityServiceTest(
    @Autowired private val operationsObservabilityService: OperationsObservabilityService,
    @Autowired private val jnsClientStub: JnsClientStub
) {

    @Test
    fun testObserveSubmitted() {
        val operation = AccountsQuotasOperationsModel.builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(UUID.randomUUID().toString())
            .setCreateDateTime(Instant.now())
            .setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.CREATE_ACCOUNT)
            .setAuthorUserId(UUID.randomUUID().toString())
            .setProviderId(TestProviders.YDB_ID)
            .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.WAITING)
            .setRequestedChanges(OperationChangesModel.builder()
                .accountCreateParams(OperationChangesModel.AccountCreateParams("test", "test",
                    UUID.randomUUID().toString(), UUID.randomUUID().toString(), false, null))
                .build())
            .setOrders(OperationOrdersModel.builder()
                .submitOrder(1L)
                .build())
            .build()
        operationsObservabilityService.observeOperationSubmitted(operation)
        Assertions.assertEquals(0L, jnsClientStub.getCounter())
    }

    @Test
    fun testObserveTransientFailure() {
        val operation = AccountsQuotasOperationsModel.builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(UUID.randomUUID().toString())
            .setCreateDateTime(Instant.now())
            .setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.CREATE_ACCOUNT)
            .setAuthorUserId(UUID.randomUUID().toString())
            .setProviderId(TestProviders.YDB_ID)
            .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.WAITING)
            .setRequestedChanges(OperationChangesModel.builder()
                .accountCreateParams(OperationChangesModel.AccountCreateParams("test", "test",
                    UUID.randomUUID().toString(), UUID.randomUUID().toString(), false, null))
                .build())
            .setOrders(OperationOrdersModel.builder()
                .submitOrder(1L)
                .build())
            .build()
        operationsObservabilityService.observeOperationTransientFailure(operation)
        Assertions.assertEquals(0L, jnsClientStub.getCounter())
    }

    @Test
    fun testObserveFinishedSuccess() {
        val operation = AccountsQuotasOperationsModel.builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(UUID.randomUUID().toString())
            .setCreateDateTime(Instant.now())
            .setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.CREATE_ACCOUNT)
            .setAuthorUserId(UUID.randomUUID().toString())
            .setProviderId(TestProviders.YDB_ID)
            .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.OK)
            .setUpdateDateTime(Instant.now().plus(1L, ChronoUnit.MINUTES))
            .setRequestedChanges(OperationChangesModel.builder()
                .accountCreateParams(OperationChangesModel.AccountCreateParams("test", "test",
                    UUID.randomUUID().toString(), UUID.randomUUID().toString(), false, null))
                .build())
            .setOrders(OperationOrdersModel.builder()
                .submitOrder(1L)
                .closeOrder(2L)
                .build())
            .build()
        operationsObservabilityService.observeOperationFinished(operation)
        Assertions.assertEquals(0L, jnsClientStub.getCounter())
    }

    @Test
    fun testObserveFinishedFailure() {
        val operation = AccountsQuotasOperationsModel.builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(UUID.randomUUID().toString())
            .setCreateDateTime(Instant.now())
            .setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.CREATE_ACCOUNT)
            .setAuthorUserId(UUID.randomUUID().toString())
            .setProviderId(TestProviders.YDB_ID)
            .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.ERROR)
            .setUpdateDateTime(Instant.now().plus(1L, ChronoUnit.MINUTES))
            .setErrorKind(OperationErrorKind.EXPIRED)
            .setRequestedChanges(OperationChangesModel.builder()
                .accountCreateParams(OperationChangesModel.AccountCreateParams("test", "test",
                    UUID.randomUUID().toString(), UUID.randomUUID().toString(), false, null))
                .build())
            .setOrders(OperationOrdersModel.builder()
                .submitOrder(1L)
                .closeOrder(2L)
                .build())
            .build()
        operationsObservabilityService.observeOperationFinished(operation)
        Assertions.assertEquals(1L, jnsClientStub.getCounter())
    }

    @Test
    fun testObserveFinishedFailureTransfer() {
        val operation = AccountsQuotasOperationsModel.builder()
            .setTenantId(Tenants.DEFAULT_TENANT_ID)
            .setOperationId(UUID.randomUUID().toString())
            .setCreateDateTime(Instant.now())
            .setOperationSource(OperationSource.USER)
            .setOperationType(AccountsQuotasOperationsModel.OperationType.MOVE_PROVISION)
            .setAuthorUserId(UUID.randomUUID().toString())
            .setProviderId(TestProviders.YDB_ID)
            .setRequestStatus(AccountsQuotasOperationsModel.RequestStatus.ERROR)
            .setUpdateDateTime(Instant.now().plus(1L, ChronoUnit.MINUTES))
            .setErrorKind(OperationErrorKind.EXPIRED)
            .setRequestedChanges(OperationChangesModel.builder()
                .transferRequestId(UUID.randomUUID().toString())
                .build())
            .setOrders(OperationOrdersModel.builder()
                .submitOrder(1L)
                .closeOrder(2L)
                .build())
            .build()
        operationsObservabilityService.observeOperationFinished(operation)
        Assertions.assertEquals(1L, jnsClientStub.getCounter())
    }

}
