package ru.yandex.intranet.d.web.front.transfer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestUsers.SERVICE_1_QUOTA_MANAGER_UID
import ru.yandex.intranet.d.TestUsers.USER_1_UID
import ru.yandex.intranet.d.TestUsers.USER_2_UID
import ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao
import ru.yandex.intranet.d.dao.quotas.QuotasDao
import ru.yandex.intranet.d.dao.transfers.TransferRequestsDao
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel
import ru.yandex.intranet.d.model.transfers.TransferRequestStatus
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateProvisionTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateReserveTransferDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto

@IntegrationTest
class FrontTransferRequestDelayedValidationTest @Autowired constructor(
    private val helper: FrontTransferRequestsHelper,
    private val webClient: WebTestClient,
    private val quotasDao: QuotasDao,
    private val transferRequestDao: TransferRequestsDao,
    private val accountsQuotasDao: AccountsQuotasDao
) {
    @Test
    fun createQuotaTransferWithDelayedValidationTest() {
        val data = helper.prepareData(sourceBalance = 100L)
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderOne.id)
                            .destinationServiceId("1")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-200")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderTwo.id)
                            .destinationServiceId("2")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("200")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val result = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.PENDING, result.transfer.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, result.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        assertTrue(transfer.appliedAt.isEmpty)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        val destinationQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderTwo.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
        assertEquals(data.quotaTwo, destinationQuota)
    }

    @Test
    fun createQuotaTransferWithDelayedValidationAndConfirmationTest() {
        val data = helper.prepareData(sourceBalance = 100L)
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .addConfirmation(true)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderOne.id)
                            .destinationServiceId("1")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-200")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderTwo.id)
                            .destinationServiceId("2")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("200")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val result = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.FAILED, result.transfer.status)
        assertTrue(result.transfer.applicationDetails.isPresent)
        assertTrue(result.transfer.applicationDetails.get().transferErrors.isPresent)
        assertEquals(1, result.transfer.applicationDetails.get().transferErrors.get().errors.size)
        assertEquals(
            """
            The balance is not enough to transfer the specified amount of quota for the resource "Test" in folder "Test".
            400 B has been provided to the provider, it must be lifted to the balance to transfer it.
            The balance is 100 B.
            """.trimIndent(),
            result.transfer.applicationDetails.get().transferErrors.get().errors[0]
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, result.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        val destinationQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderTwo.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
        assertEquals(data.quotaTwo, destinationQuota)
    }

    @Test
    fun putQuotaTransferWithDelayedValidationTest() {
        val data = helper.prepareData(sourceBalance = 100L)
        val createRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderOne.id)
                            .destinationServiceId("1")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-200")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderTwo.id)
                            .destinationServiceId("2")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("200")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val putRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderOne.id)
                            .destinationServiceId("1")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-250")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderTwo.id)
                            .destinationServiceId("2")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("250")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val createResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        val putResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .put()
            .uri(
                "/front/transfers/{id}?version={version}&delayValidation=true",
                createResponse.transfer.id, createResponse.transfer.version
            )
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.PENDING, createResponse.transfer.status)
        assertEquals(TransferRequestStatusDto.PENDING, putResponse.transfer.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, putResponse.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        assertTrue(transfer.appliedAt.isEmpty)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        val destinationQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderTwo.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
        assertEquals(data.quotaTwo, destinationQuota)
    }

    @Test
    fun putQuotaTransferWithDelayedValidationAndConfirmationTest() {
        val data = helper.prepareData(sourceBalance = 100L)
        val createRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderOne.id)
                            .destinationServiceId("1")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-200")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderTwo.id)
                            .destinationServiceId("2")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("200")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val putRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .addConfirmation(true)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderOne.id)
                            .destinationServiceId("1")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-250")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(data.folderTwo.id)
                            .destinationServiceId("2")
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("250")
                                    .resourceId(data.resource.id)
                                    .deltaUnitId(data.resource.baseUnitId)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        val createResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        val putResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .put()
            .uri(
                "/front/transfers/{id}?version={version}&delayValidation=true",
                createResponse.transfer.id, createResponse.transfer.version
            )
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.PENDING, createResponse.transfer.status)
        assertEquals(TransferRequestStatusDto.FAILED, putResponse.transfer.status)
        assertTrue(putResponse.transfer.applicationDetails.isPresent)
        assertTrue(putResponse.transfer.applicationDetails.get().transferErrors.isPresent)
        assertEquals(1, putResponse.transfer.applicationDetails.get().transferErrors.get().errors.size)
        assertEquals(
            """
            The balance is not enough to transfer the specified amount of quota for the resource "Test" in folder "Test".
            400 B has been provided to the provider, it must be lifted to the balance to transfer it.
            The balance is 100 B.
            """.trimIndent(),
            putResponse.transfer.applicationDetails.get().transferErrors.get().errors[0]
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, putResponse.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        val destinationQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderTwo.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
        assertEquals(data.quotaTwo, destinationQuota)
    }

    @Test
    fun createProvisionTransferWithDelayedValidationTest() {
        val data = helper.prepareData(sourceProvided = 100L)
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(
                        FrontCreateProvisionTransferDto(
                            sourceAccountId = data.accountOne.id,
                            sourceFolderId = data.folderOne.id,
                            sourceServiceId = data.folderOne.serviceId.toString(),
                            destinationAccountId = data.accountTwo.id,
                            destinationFolderId = data.folderTwo.id,
                            destinationServiceId = data.folderTwo.serviceId.toString(),
                            sourceAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "-200",
                                    data.resource.baseUnitId
                                )
                            ),
                            destinationAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "200",
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.PENDING, response.transfer.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        val sourceAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountOne.id, data.resource.id),
                DEFAULT_TENANT_ID
            )
        }!!.get()
        val destinationAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountTwo.id, data.resource.id),
                DEFAULT_TENANT_ID
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
    fun createProvisionTransferWithDelayedValidationAndConfirmationTest() {
        val data = helper.prepareData(sourceProvided = 150L, sourceAllocated = 50L, sourceFrozen = 50L)
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(
                        FrontCreateProvisionTransferDto(
                            sourceAccountId = data.accountOne.id,
                            sourceFolderId = data.folderOne.id,
                            sourceServiceId = data.folderOne.serviceId.toString(),
                            destinationAccountId = data.accountTwo.id,
                            destinationFolderId = data.folderTwo.id,
                            destinationServiceId = data.folderTwo.serviceId.toString(),
                            sourceAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "-200",
                                    data.resource.baseUnitId
                                )
                            ),
                            destinationAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "200",
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.FAILED, response.transfer.status)
        assertEquals(1, response.transfer.applicationDetails.get().transferErrors.get().errors.size)
        assertEquals(
            """
                The balance is not enough to transfer the specified amount of quota for the resource "Test" in account "Test".
                150 B is provided to the provider.
                50 B is allocated in the provider.
                50 B is in the process of transfer in another transfer request.
                The available amount is 50 B.
            """.trimIndent(),
            response.transfer.applicationDetails.get().transferErrors.get().errors[0]
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountOne.id, data.resource.id),
                DEFAULT_TENANT_ID
            )
        }!!.get()
        val destinationAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountTwo.id, data.resource.id),
                DEFAULT_TENANT_ID
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
    fun putProvisionTransferWithDelayedValidationTest() {
        val data = helper.prepareData(sourceProvided = 100L)
        val createRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(
                        FrontCreateProvisionTransferDto(
                            sourceAccountId = data.accountOne.id,
                            sourceFolderId = data.folderOne.id,
                            sourceServiceId = data.folderOne.serviceId.toString(),
                            destinationAccountId = data.accountTwo.id,
                            destinationFolderId = data.folderTwo.id,
                            destinationServiceId = data.folderTwo.serviceId.toString(),
                            sourceAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "-200",
                                    data.resource.baseUnitId
                                )
                            ),
                            destinationAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "200",
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val putRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(
                        FrontCreateProvisionTransferDto(
                            sourceAccountId = data.accountOne.id,
                            sourceFolderId = data.folderOne.id,
                            sourceServiceId = data.folderOne.serviceId.toString(),
                            destinationAccountId = data.accountTwo.id,
                            destinationFolderId = data.folderTwo.id,
                            destinationServiceId = data.folderTwo.serviceId.toString(),
                            sourceAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "-250",
                                    data.resource.baseUnitId
                                )
                            ),
                            destinationAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "250",
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val createResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        val putResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .put()
            .uri(
                "/front/transfers/{id}?version={version}&delayValidation=true",
                createResponse.transfer.id, createResponse.transfer.version
            )
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.PENDING, createResponse.transfer.status)
        assertEquals(TransferRequestStatusDto.PENDING, putResponse.transfer.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, putResponse.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        assertTrue(transfer.appliedAt.isEmpty)
        val sourceAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountOne.id, data.resource.id),
                DEFAULT_TENANT_ID
            )
        }!!.get()
        val destinationAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountTwo.id, data.resource.id),
                DEFAULT_TENANT_ID
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
    fun putProvisionTransferWithDelayedValidationAndConfirmationTest() {
        val data = helper.prepareData(sourceProvided = 150L, sourceAllocated = 50L, sourceFrozen = 50L)
        val createRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(
                        FrontCreateProvisionTransferDto(
                            sourceAccountId = data.accountOne.id,
                            sourceFolderId = data.folderOne.id,
                            sourceServiceId = data.folderOne.serviceId.toString(),
                            destinationAccountId = data.accountTwo.id,
                            destinationFolderId = data.folderTwo.id,
                            destinationServiceId = data.folderTwo.serviceId.toString(),
                            sourceAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "-200",
                                    data.resource.baseUnitId
                                )
                            ),
                            destinationAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "200",
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val putRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
            .addConfirmation(true)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addProvisionTransfer(
                        FrontCreateProvisionTransferDto(
                            sourceAccountId = data.accountOne.id,
                            sourceFolderId = data.folderOne.id,
                            sourceServiceId = data.folderOne.serviceId.toString(),
                            destinationAccountId = data.accountTwo.id,
                            destinationFolderId = data.folderTwo.id,
                            destinationServiceId = data.folderTwo.serviceId.toString(),
                            sourceAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "-250",
                                    data.resource.baseUnitId
                                )
                            ),
                            destinationAccountTransfers = listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id, "250",
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val createResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        val putResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .put()
            .uri(
                "/front/transfers/{id}?version={version}&delayValidation=true",
                createResponse.transfer.id, createResponse.transfer.version
            )
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.PENDING, createResponse.transfer.status)
        assertEquals(TransferRequestStatusDto.FAILED, putResponse.transfer.status)
        assertEquals(1, putResponse.transfer.applicationDetails.get().transferErrors.get().errors.size)
        assertEquals(
            """
                The balance is not enough to transfer the specified amount of quota for the resource "Test" in account "Test".
                150 B is provided to the provider.
                50 B is allocated in the provider.
                50 B is in the process of transfer in another transfer request.
                The available amount is 50 B.
            """.trimIndent(),
            putResponse.transfer.applicationDetails.get().transferErrors.get().errors[0]
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, putResponse.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountOne.id, data.resource.id),
                DEFAULT_TENANT_ID
            )
        }!!.get()
        val destinationAccountQuota = helper.rwTx {
            accountsQuotasDao.getById(
                it, AccountsQuotasModel.Identity(data.accountTwo.id, data.resource.id),
                DEFAULT_TENANT_ID
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
    fun createReserveTransferWithDelayedValidationTest() {
        val data = helper.prepareDataForReserveTransfer()
        val createRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        FrontCreateReserveTransferDto(
                            data.provider.id,
                            data.folderTwo.id,
                            data.folderTwo.serviceId.toString(),
                            listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id,
                                    (data.quotaOne.balance + 200L).toString(),
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.PENDING, response.transfer.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        assertTrue(transfer.appliedAt.isEmpty)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
    }

    @Test
    fun createReserveTransferWithDelayedValidationAndConfirmationTest() {
        val data = helper.prepareDataForReserveTransfer()
        val createRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .addConfirmation(true)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        FrontCreateReserveTransferDto(
                            data.provider.id,
                            data.folderTwo.id,
                            data.folderTwo.serviceId.toString(),
                            listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id,
                                    (data.quotaOne.balance + 200L).toString(),
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val response = webClient
            .mutateWith(MockUser.uid(USER_1_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.FAILED, response.transfer.status)
        assertTrue(response.transfer.applicationDetails.isPresent)
        assertTrue(response.transfer.applicationDetails.get().transferErrors.isPresent)
        assertEquals(1, response.transfer.applicationDetails.get().transferErrors.get().errors.size)
        assertEquals(
            """
            The balance is not enough to transfer the specified amount of quota for the resource "Test" in folder "Test".
            The balance is 150000 B.
            """.trimIndent(),
            response.transfer.applicationDetails.get().transferErrors.get().errors[0]
        )
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, response.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.FAILED, transfer.status)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
    }

    @Test
    fun putReserveTransferWithDelayedValidationTest() {
        val data = helper.prepareDataForReserveTransfer()
        val createRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        FrontCreateReserveTransferDto(
                            data.provider.id,
                            data.folderTwo.id,
                            data.folderTwo.serviceId.toString(),
                            listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id,
                                    (data.quotaOne.balance + 200L).toString(),
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val putRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        FrontCreateReserveTransferDto(
                            data.provider.id,
                            data.folderTwo.id,
                            data.folderTwo.serviceId.toString(),
                            listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id,
                                    (data.quotaOne.balance + 400L).toString(),
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val createResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        val putResponse = webClient
            .mutateWith(MockUser.uid(SERVICE_1_QUOTA_MANAGER_UID))
            .put()
            .uri(
                "/front/transfers/{id}?version={version}&delayValidation=true",
                createResponse.transfer.id, createResponse.transfer.version
            )
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.PENDING, createResponse.transfer.status)
        assertEquals(TransferRequestStatusDto.PENDING, putResponse.transfer.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, putResponse.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        assertTrue(transfer.appliedAt.isEmpty)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
    }

    @Test
    fun putReserveTransferWithDelayedValidationAndConfirmationTest() {
        val data = helper.prepareDataForReserveTransfer()
        val createRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        FrontCreateReserveTransferDto(
                            data.provider.id,
                            data.folderTwo.id,
                            data.folderTwo.serviceId.toString(),
                            listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id,
                                    (data.quotaOne.balance + 200L).toString(),
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val putRequest = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .addConfirmation(true)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        FrontCreateReserveTransferDto(
                            data.provider.id,
                            data.folderTwo.id,
                            data.folderTwo.serviceId.toString(),
                            listOf(
                                FrontCreateQuotaResourceTransferDto(
                                    data.resource.id,
                                    (data.quotaOne.balance + 400L).toString(),
                                    data.resource.baseUnitId
                                )
                            )
                        )
                    )
                    .build()
            )
            .build()

        val createResponse = webClient
            .mutateWith(MockUser.uid(USER_2_UID))
            .post()
            .uri("/front/transfers?delayValidation=true")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(createRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        val putResponse = webClient
            .mutateWith(MockUser.uid(USER_2_UID))
            .put()
            .uri(
                "/front/transfers/{id}?version={version}&delayValidation=true",
                createResponse.transfer.id, createResponse.transfer.version
            )
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(putRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody!!

        assertEquals(TransferRequestStatusDto.PENDING, createResponse.transfer.status)
        assertEquals(TransferRequestStatusDto.PENDING, putResponse.transfer.status)
        val transfer = helper.rwTx {
            transferRequestDao.getById(it, putResponse.transfer.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(TransferRequestStatus.PENDING, transfer.status)
        assertTrue(transfer.appliedAt.isEmpty)
        val sourceQuota = helper.rwTx {
            quotasDao.getOneQuota(it, data.folderOne.id, data.provider.id, data.resource.id, DEFAULT_TENANT_ID)
        }!!.get()
        assertEquals(data.quotaOne, sourceQuota)
    }
}
