package ru.yandex.intranet.d.web.api.transfers

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.transfers.TransferRequestsDao
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.transfers.TransferRequestStatus
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateProvisionTransferDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateQuotaResourceTransferDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateQuotaTransferDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveResourceTransferDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateReserveTransferDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.api.CreateTransferRequestParametersDto
import ru.yandex.intranet.d.web.model.transfers.api.TransferRequestDto

@IntegrationTest
class PublicApiTransferRequestDelayedValidationTest @Autowired constructor(
    private val helper: PublicApiTransferRequestsHelper,
    private val webClient: WebTestClient,
    private val quotasDao: QuotasDao,
    private val transferRequestDao: TransferRequestsDao,
    private val accountsQuotasDao: AccountsQuotasDao
) {
    @Test
    fun createQuotaTransferWithDelayedValidationTest() {
        val data = helper.prepareData(sourceBalance = 100L)
        val request = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .addConfirmation(false)
            .parameters(
                CreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        CreateQuotaTransferDto.builder()
                            .folderId(data.folderOne.id)
                            .addResourceTransfer(
                                CreateQuotaResourceTransferDto.builder()
                                    .delta(-200L)
                                    .resourceId(data.resource.id)
                                    .deltaUnitKey("bytes")
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfer(
                        CreateQuotaTransferDto.builder()
                            .folderId(data.folderTwo.id)
                            .addResourceTransfer(
                                CreateQuotaResourceTransferDto.builder()
                                    .delta(200L)
                                    .resourceId(data.resource.id)
                                    .deltaUnitKey("bytes")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(TransferRequestStatusDto.PENDING, response.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(TransferRequestStatus.PENDING, transfer.status)
        Assertions.assertTrue(transfer.appliedAt.isEmpty)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        val destinationQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderTwo.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(data.quotaOne, sourceQuota)
        Assertions.assertEquals(data.quotaTwo, destinationQuota)
    }

    @Test
    fun createQuotaTransferWithDelayedValidationAndConfirmationTest() {
        val data = helper.prepareData(sourceBalance = 100L)
        val request = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .addConfirmation(true)
            .parameters(
                CreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        CreateQuotaTransferDto.builder()
                            .folderId(data.folderOne.id)
                            .addResourceTransfer(
                                CreateQuotaResourceTransferDto.builder()
                                    .delta(-200L)
                                    .resourceId(data.resource.id)
                                    .deltaUnitKey("bytes")
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfer(
                        CreateQuotaTransferDto.builder()
                            .folderId(data.folderTwo.id)
                            .addResourceTransfer(
                                CreateQuotaResourceTransferDto.builder()
                                    .delta(200L)
                                    .resourceId(data.resource.id)
                                    .deltaUnitKey("bytes")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(TransferRequestStatusDto.FAILED, response.status)
        Assertions.assertTrue(response.application.isPresent)
        Assertions.assertTrue(response.application.get().failures.isPresent)
        Assertions.assertEquals(1, response.application.get().failures.get().errors.size)
        Assertions.assertEquals(
            """
            The balance is not enough to transfer the specified amount of quota for the resource "Test" in folder "Test".
            400 B has been provided to the provider, it must be lifted to the balance to transfer it.
            The balance is 100 B.
            """.trimIndent(),
            response.application.get().failures.get().errors[0]
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        val destinationQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderTwo.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(data.quotaOne, sourceQuota)
        Assertions.assertEquals(data.quotaTwo, destinationQuota)
    }

    @Test
    fun createProvisionTransferWithDelayedValidationTest() {
        val data = helper.prepareData(sourceProvided = 100L)
        val request = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(false)
            .parameters(
                CreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(
                        CreateProvisionTransferDto(
                            sourceAccountId = data.accountOne.id,
                            sourceFolderId = data.folderOne.id,
                            destinationAccountId = data.accountTwo.id,
                            destinationFolderId = data.folderTwo.id,
                            sourceAccountTransfers = listOf(
                                CreateQuotaResourceTransferDto(
                                    data.provider.id,
                                    data.resource.id,
                                    -200L,
                                    "bytes"
                                )
                            ),
                            destinationAccountTransfers = listOf(
                                CreateQuotaResourceTransferDto(
                                    data.provider.id,
                                    data.resource.id,
                                    200L,
                                    "bytes"
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(TransferRequestStatusDto.PENDING, response.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(TransferRequestStatus.PENDING, transfer.status)
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
        Assertions.assertEquals(data.accountQuotaOne.providedQuota, sourceAccountQuota.providedQuota)
        Assertions.assertEquals(data.accountQuotaOne.allocatedQuota, sourceAccountQuota.allocatedQuota)
        Assertions.assertEquals(data.accountQuotaOne.frozenProvidedQuota, sourceAccountQuota.frozenProvidedQuota)
        Assertions.assertEquals(data.accountQuotaTwo.providedQuota, destinationAccountQuota.providedQuota)
        Assertions.assertEquals(data.accountQuotaTwo.allocatedQuota, destinationAccountQuota.allocatedQuota)
        Assertions.assertEquals(data.accountQuotaTwo.frozenProvidedQuota, destinationAccountQuota.frozenProvidedQuota)
    }

    @Test
    fun createProvisionTransferWithDelayedValidationAndConfirmationTest() {
        val data = helper.prepareData(sourceProvided = 150L)
        val request = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .parameters(
                CreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(
                        CreateProvisionTransferDto(
                            sourceAccountId = data.accountOne.id,
                            sourceFolderId = data.folderOne.id,
                            destinationAccountId = data.accountTwo.id,
                            destinationFolderId = data.folderTwo.id,
                            sourceAccountTransfers = listOf(
                                CreateQuotaResourceTransferDto(
                                    data.provider.id,
                                    data.resource.id,
                                    -200L,
                                    "bytes"
                                )
                            ),
                            destinationAccountTransfers = listOf(
                                CreateQuotaResourceTransferDto(
                                    data.provider.id,
                                    data.resource.id,
                                    200L,
                                    "bytes"
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(TransferRequestStatusDto.FAILED, response.status)
        Assertions.assertTrue(response.application.isPresent)
        Assertions.assertTrue(response.application.get().failures.isPresent)
        Assertions.assertEquals(1, response.application.get().failures.get().errors.size)
        Assertions.assertEquals(
            """
            The balance is not enough to transfer the specified amount of quota for the resource "Test" in account "Test".
            150 B is provided to the provider.
            50 B is allocated in the provider.
            50 B is in the process of transfer in another transfer request.
            The available amount is 50 B.
            """.trimIndent(),
            response.application.get().failures.get().errors[0]
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(TransferRequestStatus.FAILED, transfer.status)
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
        Assertions.assertEquals(data.accountQuotaOne.providedQuota, sourceAccountQuota.providedQuota)
        Assertions.assertEquals(data.accountQuotaOne.allocatedQuota, sourceAccountQuota.allocatedQuota)
        Assertions.assertEquals(data.accountQuotaOne.frozenProvidedQuota, sourceAccountQuota.frozenProvidedQuota)
        Assertions.assertEquals(data.accountQuotaTwo.providedQuota, destinationAccountQuota.providedQuota)
        Assertions.assertEquals(data.accountQuotaTwo.allocatedQuota, destinationAccountQuota.allocatedQuota)
        Assertions.assertEquals(data.accountQuotaTwo.frozenProvidedQuota, destinationAccountQuota.frozenProvidedQuota)
    }

    @Test
    fun createReserveTransferWithDelayedValidationTest() {
        val data = helper.prepareDataForReserveTransfer()
        val createRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .addConfirmation(false)
            .parameters(
                CreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        CreateReserveTransferDto(
                            data.provider.id,
                            data.folderTwo.id,
                            listOf(
                                CreateReserveResourceTransferDto(
                                    data.resource.id,
                                    data.quotaOne.balance + 200L,
                                    "bytes"
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(TestUsers.SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/api/v1/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(TransferRequestStatusDto.PENDING, response.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(TransferRequestStatus.PENDING, transfer.status)
        Assertions.assertTrue(transfer.appliedAt.isEmpty)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(data.quotaOne, sourceQuota)
    }

    @Test
    fun createReserveTransferWithDelayedValidationAndConfirmationTest() {
        val data = helper.prepareDataForReserveTransfer()
        val createRequest = CreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .addConfirmation(true)
            .parameters(
                CreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        CreateReserveTransferDto(
                            data.provider.id,
                            data.folderTwo.id,
                            listOf(
                                CreateReserveResourceTransferDto(
                                    data.resource.id,
                                    data.quotaOne.balance + 200L,
                                    "bytes"
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .post()
            .uri("/api/v1/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(TransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        Assertions.assertEquals(TransferRequestStatusDto.FAILED, response.status)
        Assertions.assertTrue(response.application.isPresent)
        Assertions.assertTrue(response.application.get().failures.isPresent)
        Assertions.assertEquals(1, response.application.get().failures.get().errors.size)
        Assertions.assertEquals(
            """
            The balance is not enough to transfer the specified amount of quota for the resource "Test" in folder "Test".
            The balance is 150000 B.
            """.trimIndent(),
            response.application.get().failures.get().errors[0]
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, Tenants.DEFAULT_TENANT_ID)
        }!!.get()
        Assertions.assertEquals(data.quotaOne, sourceQuota)
    }
}
