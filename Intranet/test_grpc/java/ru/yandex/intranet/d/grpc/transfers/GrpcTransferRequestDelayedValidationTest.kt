package ru.yandex.intranet.d.grpc.transfers

import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_UID
import ru.yandex.intranet.d.TestUsers.USER_1_UID
import ru.yandex.intranet.d.TestUsers.USER_3_UID
import ru.yandex.intranet.d.backend.service.proto.CreateTransferRequest
import ru.yandex.intranet.d.backend.service.proto.ProvisionTransfer
import ru.yandex.intranet.d.backend.service.proto.ProvisionTransferParameters
import ru.yandex.intranet.d.backend.service.proto.QuotaTransfer
import ru.yandex.intranet.d.backend.service.proto.QuotaTransferParameters
import ru.yandex.intranet.d.backend.service.proto.ReserveResourceTransfer
import ru.yandex.intranet.d.backend.service.proto.ReserveTransferParameters
import ru.yandex.intranet.d.backend.service.proto.ResourceTransfer
import ru.yandex.intranet.d.backend.service.proto.TransferAmount
import ru.yandex.intranet.d.backend.service.proto.TransferParameters
import ru.yandex.intranet.d.backend.service.proto.TransferStatus
import ru.yandex.intranet.d.backend.service.proto.TransferType
import ru.yandex.intranet.d.backend.service.proto.TransfersServiceGrpc
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.transfers.TransferRequestsDao
import ru.yandex.intranet.d.grpc.MockGrpcUser
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.transfers.TransferRequestStatus

@IntegrationTest
class GrpcTransferRequestDelayedValidationTest @Autowired constructor(
    private val helper: GrpcTransferRequestsHelper,
    private val quotasDao: QuotasDao,
    private val transferRequestDao: TransferRequestsDao,
    private val accountsQuotasDao: AccountsQuotasDao
) {

    @GrpcClient("inProcess")
    lateinit var transfersService: TransfersServiceGrpc.TransfersServiceBlockingStub

    @Test
    fun createQuotaTransferDelayedValidationTest() {
        val data = helper.prepareData(sourceBalance = 100L)
        val transferParameters = TransferParameters.newBuilder()
            .setQuotaTransfer(
                QuotaTransferParameters.newBuilder()
                    .addQuotaTransfers(
                        QuotaTransfer.newBuilder()
                            .setFolderId(data.folderOne.id)
                            .addResourceTransfers(
                                ResourceTransfer.newBuilder()
                                    .setDelta(
                                        TransferAmount.newBuilder()
                                            .setValue(-200L)
                                            .setUnitKey("bytes")
                                            .build()
                                    )
                                    .setResourceId(data.resource.id)
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfers(
                        QuotaTransfer.newBuilder()
                            .setFolderId(data.folderTwo.id)
                            .addResourceTransfers(
                                ResourceTransfer.newBuilder()
                                    .setDelta(
                                        TransferAmount.newBuilder()
                                            .setValue(200L)
                                            .setUnitKey("bytes")
                                            .build()
                                    )
                                    .setResourceId(data.resource.id)
                                    .build()
                            )
                            .build()
                    )
            )
            .build()
        val response = transfersService
            .withCallCredentials(MockGrpcUser.uid(USER_3_UID))
            .createTransfer(
                CreateTransferRequest.newBuilder()
                    .setType(TransferType.QUOTA_TRANSFER)
                    .setAddConfirmation(false)
                    .setDelayValidation(true)
                    .setParameters(transferParameters)
                    .build()
            )
        assertEquals(TransferStatus.PENDING, response.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transferId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        Assertions.assertTrue(transfer.appliedAt.isEmpty)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        val destinationQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderTwo.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
        assertEquals(data.quotaTwo, destinationQuota)
    }

    @Test
    fun createQuotaTransferDelayedValidationAndConfirmationTest() {
        val data = helper.prepareData(sourceBalance = 100L)
        val transferParameters = TransferParameters.newBuilder()
            .setQuotaTransfer(
                QuotaTransferParameters.newBuilder()
                    .addQuotaTransfers(
                        QuotaTransfer.newBuilder()
                            .setFolderId(data.folderOne.id)
                            .addResourceTransfers(
                                ResourceTransfer.newBuilder()
                                    .setDelta(
                                        TransferAmount.newBuilder()
                                            .setValue(-200L)
                                            .setUnitKey("bytes")
                                            .build()
                                    )
                                    .setResourceId(data.resource.id)
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfers(
                        QuotaTransfer.newBuilder()
                            .setFolderId(data.folderTwo.id)
                            .addResourceTransfers(
                                ResourceTransfer.newBuilder()
                                    .setDelta(
                                        TransferAmount.newBuilder()
                                            .setValue(200L)
                                            .setUnitKey("bytes")
                                            .build()
                                    )
                                    .setResourceId(data.resource.id)
                                    .build()
                            )
                            .build()
                    )
            )
            .build()
        val response = transfersService
            .withCallCredentials(MockGrpcUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .createTransfer(
                CreateTransferRequest.newBuilder()
                    .setType(TransferType.QUOTA_TRANSFER)
                    .setAddConfirmation(true)
                    .setDelayValidation(true)
                    .setParameters(transferParameters)
                    .build()
            )
        assertEquals(TransferStatus.FAILED, response.status)
        assertEquals(1, response.application.failures.errorsCount)
        assertEquals(
            """
            The balance is not enough to transfer the specified amount of quota for the resource "Test" in folder "Test".
            400 B has been provided to the provider, it must be lifted to the balance to transfer it.
            The balance is 100 B.
            """.trimIndent(),
            response.application.failures.getErrors(0)
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transferId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        val destinationQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderTwo.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
        assertEquals(data.quotaTwo, destinationQuota)
    }

    @Test
    fun createProvisionTransferDelayedValidationTest() {
        val data = helper.prepareData(sourceProvided = 100L)
        val response = transfersService
            .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
            .createTransfer(
                CreateTransferRequest.newBuilder()
                    .setType(TransferType.PROVISION_TRANSFER)
                    .setAddConfirmation(false)
                    .setDelayValidation(true)
                    .setParameters(
                        TransferParameters.newBuilder()
                            .setProvisionTransfer(
                                ProvisionTransferParameters.newBuilder()
                                    .addProvisionTransfer(
                                        ProvisionTransfer.newBuilder()
                                            .setSourceFolderId(data.folderOne.id)
                                            .setSourceAccountId(data.accountOne.id)
                                            .setDestinationFolderId(data.folderTwo.id)
                                            .setDestinationAccountId(data.accountTwo.id)
                                            .addSourceAccountTransfers(
                                                ResourceTransfer.newBuilder()
                                                    .setProviderId(data.provider.id)
                                                    .setResourceId(data.resource.id)
                                                    .setDelta(
                                                        TransferAmount.newBuilder()
                                                            .setValue(-200L)
                                                            .setUnitKey("bytes")
                                                    )
                                            )
                                            .addDestinationAccountTransfers(
                                                ResourceTransfer.newBuilder()
                                                    .setProviderId(data.provider.id)
                                                    .setResourceId(data.resource.id)
                                                    .setDelta(
                                                        TransferAmount.newBuilder()
                                                            .setValue(200L)
                                                            .setUnitKey("bytes")
                                                    )
                                            )
                                    )
                            )
                    )
                    .build()
            )
        assertNotNull(response)
        assertEquals(TransferStatus.PENDING, response.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transferId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        val sourceAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountOne.id, data.resource.id),
                Tenants.DEFAULT_TENANT_ID
            )
        }!!.get()
        val destinationAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountTwo.id, data.resource.id),
                Tenants.DEFAULT_TENANT_ID
            )
        }!!.get()
        assertEquals(data.accountQuotaOne.providedQuota, sourceAccountQuota.providedQuota)
        assertEquals(data.accountQuotaOne.allocatedQuota, sourceAccountQuota.allocatedQuota)
        assertEquals(data.accountQuotaOne.frozenProvidedQuota, sourceAccountQuota.frozenProvidedQuota)
        assertEquals(data.accountQuotaTwo.providedQuota, destinationAccountQuota.providedQuota)
        assertEquals(data.accountQuotaTwo.allocatedQuota, destinationAccountQuota.allocatedQuota)
        assertEquals(data.accountQuotaTwo.frozenProvidedQuota, destinationAccountQuota.frozenProvidedQuota)
    }

    @Test
    fun createProvisionTransferDelayedValidationAndConfirmationTest() {
        val data = helper.prepareData(sourceProvided = 150L)
        val response = transfersService
            .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
            .createTransfer(
                CreateTransferRequest.newBuilder()
                    .setType(TransferType.PROVISION_TRANSFER)
                    .setAddConfirmation(true)
                    .setDelayValidation(true)
                    .setParameters(
                        TransferParameters.newBuilder()
                            .setProvisionTransfer(
                                ProvisionTransferParameters.newBuilder()
                                    .addProvisionTransfer(
                                        ProvisionTransfer.newBuilder()
                                            .setSourceFolderId(data.folderOne.id)
                                            .setSourceAccountId(data.accountOne.id)
                                            .setDestinationFolderId(data.folderTwo.id)
                                            .setDestinationAccountId(data.accountTwo.id)
                                            .addSourceAccountTransfers(
                                                ResourceTransfer.newBuilder()
                                                    .setProviderId(data.provider.id)
                                                    .setResourceId(data.resource.id)
                                                    .setDelta(
                                                        TransferAmount.newBuilder()
                                                            .setValue(-200L)
                                                            .setUnitKey("bytes")
                                                    )
                                            )
                                            .addDestinationAccountTransfers(
                                                ResourceTransfer.newBuilder()
                                                    .setProviderId(data.provider.id)
                                                    .setResourceId(data.resource.id)
                                                    .setDelta(
                                                        TransferAmount.newBuilder()
                                                            .setValue(200L)
                                                            .setUnitKey("bytes")
                                                    )
                                            )
                                    )
                            )
                    )
                    .build()
            )
        assertNotNull(response)
        assertEquals(TransferStatus.FAILED, response.status)
        assertEquals(1, response.application.failures.errorsCount)
        assertEquals(
            """
            The balance is not enough to transfer the specified amount of quota for the resource "Test" in account "Test".
            150 B is provided to the provider.
            50 B is allocated in the provider.
            50 B is in the process of transfer in another transfer request.
            The available amount is 50 B.
            """.trimIndent(),
            response.application.failures.getErrors(0)
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transferId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountOne.id, data.resource.id),
                Tenants.DEFAULT_TENANT_ID
            )
        }!!.get()
        val destinationAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountTwo.id, data.resource.id),
                Tenants.DEFAULT_TENANT_ID
            )
        }!!.get()
        assertEquals(data.accountQuotaOne.providedQuota, sourceAccountQuota.providedQuota)
        assertEquals(data.accountQuotaOne.allocatedQuota, sourceAccountQuota.allocatedQuota)
        assertEquals(data.accountQuotaOne.frozenProvidedQuota, sourceAccountQuota.frozenProvidedQuota)
        assertEquals(data.accountQuotaTwo.providedQuota, destinationAccountQuota.providedQuota)
        assertEquals(data.accountQuotaTwo.allocatedQuota, destinationAccountQuota.allocatedQuota)
        assertEquals(data.accountQuotaTwo.frozenProvidedQuota, destinationAccountQuota.frozenProvidedQuota)
    }

    @Test
    fun createReserveTransferDelayedValidationTest() {
        val data = helper.prepareDataForReserveTransfer()
        val response = transfersService
            .withCallCredentials(MockGrpcUser.uid(TestUsers.USER_2_UID))
            .createTransfer(
                CreateTransferRequest.newBuilder()
                    .setType(TransferType.RESERVE_TRANSFER)
                    .setAddConfirmation(false)
                    .setDelayValidation(true)
                    .setParameters(
                        TransferParameters.newBuilder()
                            .setReserveTransfer(
                                ReserveTransferParameters.newBuilder()
                                    .setProviderId(data.provider.id)
                                    .setFolderId(data.folderTwo.id)
                                    .addResourceTransfers(
                                        ReserveResourceTransfer.newBuilder()
                                            .setDelta(
                                                TransferAmount.newBuilder()
                                                    .setValue(data.quotaOne.balance + 200L)
                                                    .setUnitKey("bytes")
                                                    .build()
                                            )
                                            .setResourceId(data.resource.id)
                                            .build()
                                    )
                            )
                            .build()
                    )
                    .build()
            )
        assertNotNull(response)
        assertEquals(TransferStatus.PENDING, response.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transferId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        Assertions.assertTrue(transfer.appliedAt.isEmpty)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
    }

    @Test
    fun createReserveTransferDelayedValidationAndConfirmationTest() {
        val data = helper.prepareDataForReserveTransfer()
        val response = transfersService
            .withCallCredentials(MockGrpcUser.uid(USER_1_UID))
            .createTransfer(
                CreateTransferRequest.newBuilder()
                    .setType(TransferType.RESERVE_TRANSFER)
                    .setAddConfirmation(true)
                    .setDelayValidation(true)
                    .setParameters(
                        TransferParameters.newBuilder()
                            .setReserveTransfer(
                                ReserveTransferParameters.newBuilder()
                                    .setProviderId(data.provider.id)
                                    .setFolderId(data.folderTwo.id)
                                    .addResourceTransfers(
                                        ReserveResourceTransfer.newBuilder()
                                            .setDelta(
                                                TransferAmount.newBuilder()
                                                    .setValue(data.quotaOne.balance + 200L)
                                                    .setUnitKey("bytes")
                                                    .build()
                                            )
                                            .setResourceId(data.resource.id)
                                            .build()
                                    )
                            )
                            .build()
                    )
                    .build()
            )
        assertNotNull(response)
        assertEquals(TransferStatus.FAILED, response.status)
        assertEquals(1, response.application.failures.errorsCount)
        assertEquals(
            """
            The balance is not enough to transfer the specified amount of quota for the resource "Test" in folder "Test".
            The balance is 150000 B.
            """.trimIndent(),
            response.application.failures.getErrors(0)
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transferId, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
    }
}
