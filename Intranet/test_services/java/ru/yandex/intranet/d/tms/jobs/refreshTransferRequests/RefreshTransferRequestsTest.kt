package ru.yandex.intranet.d.tms.jobs.refreshTransferRequests

import com.yandex.ydb.table.transaction.TransactionMode
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.http.MediaType
import reactor.core.publisher.Mono
import ru.yandex.intranet.d.*
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.model.users.*
import ru.yandex.intranet.d.services.tracker.TrackerIssueResolution
import ru.yandex.intranet.d.util.Long2LongMultimap
import ru.yandex.intranet.d.util.result.ErrorCollection
import ru.yandex.intranet.d.util.result.Result
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.tracker.TrackerCreateTicketResponseDto
import ru.yandex.intranet.d.web.model.tracker.TrackerTransitionExecuteDto
import ru.yandex.intranet.d.web.model.tracker.TrackerUpdateTicketDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestHistoryEventTypeDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestStatusDto
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto
import ru.yandex.intranet.d.web.model.transfers.front.*
import java.util.*

/**
 * Test for cron job to refresh transfer requests.
 *
 * @author Petr Surkov <petrsurkov></petrsurkov>@yandex-team.ru>
 * @see [DISPENSER-4357](https://st.yandex-team.ru/DISPENSER-4357)
 */
@IntegrationTest
class RefreshTransferRequestsTest : AbstractTransferRequestsTest() {

    @Test
    fun `Test refresh nothing`() {
        refreshTransferRequests.execute()
    }

    @Test
    fun `Test refresh does not change quota transfer when nothing changes`() {
        val ticketKey = "DISPENSERTREQ-1"
        Mockito.`when`(
            trackerClient.createTicket(Mockito.any())
        ).thenReturn(
            Mono.just(Result.success(TrackerCreateTicketResponseDto(ticketKey)))
        )
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .description("test description")
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_D.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    ).addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_DISPENSER.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    ).build()
            ).build()
        val createResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult)

        val mailsBeforeRefresh = mailSender.counter
        refreshTransferRequests.execute()
        val mailsAfterRefresh = mailSender.counter

        val getResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/transfers/{id}", createResult!!.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(getResult)
        val transfer = getResult!!.transfer
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, transfer.status)
        checkIsInPendingTransferRequest(transfer.id)
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_SERVICE_D,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER,
                TestUsers.SERVICE_1_QUOTA_MANAGER_2
            )
        )
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_2_ID,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER,
                TestUsers.SERVICE_1_QUOTA_MANAGER_2
            )
        )

        Assertions.assertEquals(
            listOf(TransferRequestHistoryEventTypeDto.CREATED),
            getHistoryEventTypes(transfer)
        )

        Mockito.verify(trackerClient, Mockito.never())
            .updateTicket(Mockito.any(), Mockito.any())
        Mockito.verify(trackerClient, Mockito.never())
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.any())
        Assertions.assertEquals(mailsBeforeRefresh, mailsAfterRefresh)
    }

    @Test
    fun `Test refresh add responsible for quota transfer request`() {
        val ticketKey = "DISPENSERTREQ-1"
        Mockito.`when`(
            trackerClient.createTicket(Mockito.any())
        ).thenReturn(
            Mono.just(Result.success(TrackerCreateTicketResponseDto(ticketKey)))
        )
        Mockito.`when`(
            trackerClient.updateTicket(ArgumentMatchers.eq(ticketKey), Mockito.any())
        ).thenReturn(
            Mono.just(Result.failure(ErrorCollection.builder().build()))
        )
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .description("test description")
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_D.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    )
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_DISPENSER.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    ).build()
            ).build()
        val createResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult)

        val newQuotaManager = with(UserModel.builder()) {
            id(UUID.randomUUID().toString())
            tenantId(Tenants.DEFAULT_TENANT_ID)
            passportUid("1001")
            passportLogin("test-login-1001")
            staffId(1001L)
            staffDismissed(false)
            staffRobot(false)
            staffAffiliation(StaffAffiliation.YANDEX)
            roles(mapOf(UserServiceRoles.QUOTA_MANAGER to setOf(TestServices.TEST_SERVICE_ID_D)))
            gender("M")
            firstNameEn("test")
            firstNameRu("test")
            lastNameEn("test")
            lastNameRu("test")
            dAdmin(false)
            deleted(false)
            workEmail("login-1001@yandex-team.ru")
            langUi("ru")
            timeZone("Europe/Moscow")
        }.build()
        val newQuotaManagerAbcServiceMemberModel = with(AbcServiceMemberModel.newBuilder()) {
            id(1001)
            staffId(1001)
            serviceId(TestServices.TEST_SERVICE_ID_D)
            roleId(quotaManagerRoleId)
            state(AbcServiceMemberState.ACTIVE)
        }.build()

        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                usersDao.upsertUserRetryable(txSession, newQuotaManager)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.upsertManyRetryable(txSession, listOf(newQuotaManagerAbcServiceMemberModel))
            }
        }.block()

        val mailsBeforeRefresh = mailSender.counter
        refreshTransferRequests.execute()
        val mailsAfterRefresh = mailSender.counter

        val getResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/transfers/{id}", createResult!!.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(getResult)
        val transfer = getResult!!.transfer
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, transfer.status)
        checkIsInPendingTransferRequest(transfer.id)
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_SERVICE_D,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER,
                TestUsers.SERVICE_1_QUOTA_MANAGER_2,
                newQuotaManager.id
            )
        )
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_2_ID,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER,
                TestUsers.SERVICE_1_QUOTA_MANAGER_2
            )
        )

        Assertions.assertEquals(
            listOf(
                TransferRequestHistoryEventTypeDto.UPDATED,
                TransferRequestHistoryEventTypeDto.CREATED
            ),
            getHistoryEventTypes(transfer)
        )

        val transferUrlWithName = "((https://abc.test.yandex-team.ru/folders/transfers/${createResult.transfer.id}" +
            " Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка))"
        val expectedUpdateTicketDto = TrackerUpdateTicketDto(
            "Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка",
            """
                Сервис-источник: https://abc.test.yandex-team.ru/services/dispenser
                Подтверждающие: staff:login-10, staff:login-12
                Сервис-получатель: https://abc.test.yandex-team.ru/services/d
                Подтверждающие: staff:login-10, staff:login-12, staff:test-login-1001
                Заявка в ABCD: $transferUrlWithName

                **Провайдер: YP**
                YP-HDD-MAN: 1 GB

                Комментарий:
                test description
                """.trimIndent(),
            listOf(1L, transferComponentId),
            listOf("dispenser", "d")
        )
        val updateTicketDtoCaptor = ArgumentCaptor.forClass(TrackerUpdateTicketDto::class.java)
        Mockito.verify(trackerClient)
            .updateTicket(Mockito.eq(ticketKey), updateTicketDtoCaptor.capture())
        Assertions.assertEquals(1, updateTicketDtoCaptor.allValues.size)
        assertEqualsUpdateTicketDtoDespiteOrderOfResponsible(expectedUpdateTicketDto, updateTicketDtoCaptor.value)
        Mockito.verify(trackerClient, Mockito.never())
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.any())
        Assertions.assertEquals(1, mailsAfterRefresh - mailsBeforeRefresh)
    }

    @Test
    fun `Test refresh remove responsible from quota transfer request`() {
        val ticketKey = "DISPENSERTREQ-1"
        Mockito.`when`(
            trackerClient.createTicket(Mockito.any())
        ).thenReturn(
            Mono.just(Result.success(TrackerCreateTicketResponseDto(ticketKey)))
        )
        Mockito.`when`(
            trackerClient.updateTicket(ArgumentMatchers.eq(ticketKey), Mockito.any())
        ).thenReturn(
            Mono.just(Result.failure(ErrorCollection.builder().build()))
        )
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .description("test description")
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_D.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    ).addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_DISPENSER.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    ).build()
            ).build()
        val createResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult)

        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                usersDao.getById(txSession, TestUsers.SERVICE_1_QUOTA_MANAGER, Tenants.DEFAULT_TENANT_ID)
                    .flatMap {
                        usersDao.updateUserRetryable(
                            txSession, it.get().copyBuilder()
                                .roles(emptyMap())
                                .build()
                        )
                    }
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.depriveRoleRetryable(
                    txSession,
                    3036L,
                )
            }
        }.block()

        val mailsBeforeRefresh = mailSender.counter
        refreshTransferRequests.execute()
        val mailsAfterRefresh = mailSender.counter

        val getResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/transfers/{id}", createResult!!.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(getResult)
        val transfer = getResult!!.transfer
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, transfer.status)
        checkIsInPendingTransferRequest(transfer.id)
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_SERVICE_D,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER_2
            )
        )
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_2_ID,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER,
                TestUsers.SERVICE_1_QUOTA_MANAGER_2
            )
        )

        Assertions.assertEquals(
            listOf(
                TransferRequestHistoryEventTypeDto.UPDATED,
                TransferRequestHistoryEventTypeDto.CREATED
            ),
            getHistoryEventTypes(transfer)
        )

        val transferUrlWithName = "((https://abc.test.yandex-team.ru/folders/transfers/${createResult.transfer.id}" +
            " Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка))"
        val expectedUpdateTicketDto = TrackerUpdateTicketDto(
            "Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка",
            """
                Сервис-источник: https://abc.test.yandex-team.ru/services/dispenser
                Подтверждающие: staff:login-10, staff:login-12
                Сервис-получатель: https://abc.test.yandex-team.ru/services/d
                Подтверждающие: staff:login-12
                Заявка в ABCD: $transferUrlWithName

                **Провайдер: YP**
                YP-HDD-MAN: 1 GB

                Комментарий:
                test description
                """.trimIndent(),
            listOf(1L, transferComponentId),
            listOf("dispenser", "d")
        )
        val updateTicketDtoCaptor = ArgumentCaptor.forClass(TrackerUpdateTicketDto::class.java)
        Mockito.verify(trackerClient)
            .updateTicket(Mockito.eq(ticketKey), updateTicketDtoCaptor.capture())
        Assertions.assertEquals(1, updateTicketDtoCaptor.allValues.size)
        assertEqualsUpdateTicketDtoDespiteOrderOfResponsible(expectedUpdateTicketDto, updateTicketDtoCaptor.value)
        Mockito.verify(trackerClient, Mockito.never())
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.any())
        Assertions.assertEquals(mailsBeforeRefresh, mailsAfterRefresh)
    }

    @Test
    fun `Test refresh does not change reserve transfer when nothing changes`() {
        val ticketKey = "DISPENSERTREQ-1"
        Mockito.`when`(
            trackerClient.createTicket(Mockito.any())
        ).thenReturn(
            Mono.just(Result.success(TrackerCreateTicketResponseDto(ticketKey)))
        )
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .description("test description")
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        FrontCreateReserveTransferDto.builder()
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("1")
                                    .resourceId(TestResources.YP_HDD_SAS)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            )
                            .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_D.toString())
                            .providerId(TestProviders.YP_ID)
                            .build()
                    ).build()
            ).build()
        val createResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult)

        val mailsBeforeRefresh = mailSender.counter
        refreshTransferRequests.execute()
        val mailsAfterRefresh = mailSender.counter

        val getResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/transfers/{id}", createResult!!.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(getResult)
        val transfer = getResult!!.transfer
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, transfer.status)
        checkIsInPendingTransferRequest(transfer.id)
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_SERVICE_D,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER,
                TestUsers.SERVICE_1_QUOTA_MANAGER_2
            )
        )

        Assertions.assertEquals(
            listOf(TransferRequestHistoryEventTypeDto.CREATED),
            getHistoryEventTypes(transfer)
        )

        Mockito.verify(trackerClient, Mockito.never())
            .updateTicket(Mockito.any(), Mockito.any())
        Mockito.verify(trackerClient, Mockito.never())
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.any())
        Assertions.assertEquals(mailsBeforeRefresh, mailsAfterRefresh)
    }

    @Test
    fun `Test refresh add responsible for reserve transfer request`() {
        val ticketKey = "DISPENSERTREQ-1"
        Mockito.`when`(
            trackerClient.createTicket(Mockito.any())
        ).thenReturn(
            Mono.just(Result.success(TrackerCreateTicketResponseDto(ticketKey)))
        )
        Mockito.`when`(
            trackerClient.updateTicket(ArgumentMatchers.eq(ticketKey), Mockito.any())
        ).thenReturn(
            Mono.just(Result.failure(ErrorCollection.builder().build()))
        )
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .description("test description")
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        FrontCreateReserveTransferDto.builder()
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("1")
                                    .resourceId(TestResources.YP_HDD_SAS)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            )
                            .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_D.toString())
                            .providerId(TestProviders.YP_ID)
                            .build()
                    ).build()
            ).build()
        val createResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult)

        val newQuotaManager = with(UserModel.builder()) {
            id(UUID.randomUUID().toString())
            tenantId(Tenants.DEFAULT_TENANT_ID)
            passportUid("1001")
            passportLogin("test-login-1001")
            staffId(1001L)
            staffDismissed(false)
            staffRobot(false)
            staffAffiliation(StaffAffiliation.YANDEX)
            roles(mapOf(UserServiceRoles.QUOTA_MANAGER to setOf(TestServices.TEST_SERVICE_ID_D)))
            gender("M")
            firstNameEn("test")
            firstNameRu("test")
            lastNameEn("test")
            lastNameRu("test")
            dAdmin(false)
            deleted(false)
            workEmail("login-1001@yandex-team.ru")
            langUi("ru")
            timeZone("Europe/Moscow")
        }.build()
        val newQuotaManagerAbcServiceMemberModel = with(AbcServiceMemberModel.newBuilder()) {
            id(1001)
            staffId(1001)
            serviceId(TestServices.TEST_SERVICE_ID_D)
            roleId(quotaManagerRoleId)
            state(AbcServiceMemberState.ACTIVE)
        }.build()

        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                usersDao.upsertUserRetryable(txSession, newQuotaManager)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.upsertManyRetryable(txSession, listOf(newQuotaManagerAbcServiceMemberModel))
            }
        }.block()

        val mailsBeforeRefresh = mailSender.counter
        refreshTransferRequests.execute()
        val mailsAfterRefresh = mailSender.counter

        val getResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/transfers/{id}", createResult!!.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(getResult)
        val transfer = getResult!!.transfer
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, transfer.status)
        checkIsInPendingTransferRequest(transfer.id)
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_SERVICE_D,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER,
                TestUsers.SERVICE_1_QUOTA_MANAGER_2,
                newQuotaManager.id
            )
        )

        Assertions.assertEquals(
            listOf(
                TransferRequestHistoryEventTypeDto.UPDATED,
                TransferRequestHistoryEventTypeDto.CREATED
            ),
            getHistoryEventTypes(transfer)
        )

        val transferUrlWithName = "((https://abc.test.yandex-team.ru/folders/transfers/${createResult.transfer.id}" +
            " Выдача квоты из резерва YP в d:Проверочная папка))"
        val expectedUpdateTicketDto = TrackerUpdateTicketDto(
            "Выдача квоты из резерва YP в d:Проверочная папка",
            """
                Сервис-получатель: https://abc.test.yandex-team.ru/services/d
                Подтверждающие: staff:login-10, staff:login-12, staff:test-login-1001
                Провайдер-источник: https://abc.test.yandex-team.ru/services/dispenser
                Подтверждающие от провайдера: staff:login-10
                Заявка в ABCD: $transferUrlWithName

                **Провайдер: YP**
                YP-HDD-SAS: 1 GB

                Комментарий:
                test description
                """.trimIndent(),
            listOf(1L, transferReserveComponentId),
            listOf("dispenser", "d")
        )
        val updateTicketDtoCaptor = ArgumentCaptor.forClass(TrackerUpdateTicketDto::class.java)
        Mockito.verify(trackerClient)
            .updateTicket(Mockito.eq(ticketKey), updateTicketDtoCaptor.capture())
        Assertions.assertEquals(1, updateTicketDtoCaptor.allValues.size)
        assertEqualsUpdateTicketDtoDespiteOrderOfResponsible(expectedUpdateTicketDto, updateTicketDtoCaptor.value)
        Mockito.verify(trackerClient, Mockito.never())
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.any())
        Assertions.assertEquals(1, mailsAfterRefresh - mailsBeforeRefresh)
    }

    @Test
    fun `Test refresh remove responsible from reserve transfer request`() {
        val ticketKey = "DISPENSERTREQ-1"
        Mockito.`when`(
            trackerClient.createTicket(Mockito.any())
        ).thenReturn(
            Mono.just(Result.success(TrackerCreateTicketResponseDto(ticketKey)))
        )
        Mockito.`when`(
            trackerClient.updateTicket(ArgumentMatchers.eq(ticketKey), Mockito.any())
        ).thenReturn(
            Mono.just(Result.failure(ErrorCollection.builder().build()))
        )
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
            .description("test description")
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .reserveTransfer(
                        FrontCreateReserveTransferDto.builder()
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("1")
                                    .resourceId(TestResources.YP_HDD_SAS)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            )
                            .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_D.toString())
                            .providerId(TestProviders.YP_ID)
                            .build()
                    ).build()
            ).build()
        val createResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult)

        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                usersDao.getById(txSession, TestUsers.SERVICE_1_QUOTA_MANAGER, Tenants.DEFAULT_TENANT_ID)
                    .flatMap {
                        usersDao.updateUserRetryable(
                            txSession, it.get().copyBuilder()
                                .roles(emptyMap())
                                .build()
                        )
                    }
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.depriveRoleRetryable(
                    txSession,
                    3036L,
                )
            }
        }.block()

        val mailsBeforeRefresh = mailSender.counter
        refreshTransferRequests.execute()
        val mailsAfterRefresh = mailSender.counter

        val getResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/transfers/{id}", createResult!!.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(getResult)
        val transfer = getResult!!.transfer
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, transfer.status)
        checkIsInPendingTransferRequest(transfer.id)
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_SERVICE_D,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER_2
            )
        )

        Assertions.assertEquals(
            listOf(
                TransferRequestHistoryEventTypeDto.UPDATED,
                TransferRequestHistoryEventTypeDto.CREATED
            ),
            getHistoryEventTypes(transfer)
        )

        val transferUrlWithName = "((https://abc.test.yandex-team.ru/folders/transfers/${createResult.transfer.id}" +
            " Выдача квоты из резерва YP в d:Проверочная папка))"
        val expectedUpdateTicketDto = TrackerUpdateTicketDto(
            "Выдача квоты из резерва YP в d:Проверочная папка",
            """
                Сервис-получатель: https://abc.test.yandex-team.ru/services/d
                Подтверждающие: staff:login-12
                Провайдер-источник: https://abc.test.yandex-team.ru/services/dispenser
                Подтверждающие от провайдера: staff:login-10
                Заявка в ABCD: $transferUrlWithName

                **Провайдер: YP**
                YP-HDD-SAS: 1 GB

                Комментарий:
                test description
                """.trimIndent(),
            listOf(1L, transferReserveComponentId),
            listOf("dispenser", "d")
        )
        val updateTicketDtoCaptor = ArgumentCaptor.forClass(TrackerUpdateTicketDto::class.java)
        Mockito.verify(trackerClient)
            .updateTicket(Mockito.eq(ticketKey), updateTicketDtoCaptor.capture())
        Assertions.assertEquals(1, updateTicketDtoCaptor.allValues.size)
        assertEqualsUpdateTicketDtoDespiteOrderOfResponsible(expectedUpdateTicketDto, updateTicketDtoCaptor.value)
        Mockito.verify(trackerClient, Mockito.never())
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.any())
        Assertions.assertEquals(mailsBeforeRefresh, mailsAfterRefresh)
    }

    @Test
    fun `Test refresh reject request when all responsible out`() {
        val ticketKey = "DISPENSERTREQ-1"
        Mockito.`when`(
            trackerClient.createTicket(Mockito.any())
        ).thenReturn(
            Mono.just(Result.success(TrackerCreateTicketResponseDto(ticketKey)))
        )
        Mockito.`when`(
            trackerClient.updateTicket(ArgumentMatchers.eq(ticketKey), Mockito.any())
        ).thenReturn(
            Mono.just(Result.failure(ErrorCollection.builder().build()))
        )
        Mockito.`when`(
            trackerClient.closeTicket(ArgumentMatchers.eq(ticketKey), Mockito.any(), Mockito.any())
        ).thenReturn(
            Mono.just(Result.failure(ErrorCollection.builder().build()))
        )
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .description("test description")
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_D.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    ).addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_DISPENSER.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    ).build()
            ).build()
        val createResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult)

        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                usersDao.getById(txSession, TestUsers.SERVICE_1_QUOTA_MANAGER, Tenants.DEFAULT_TENANT_ID)
                    .flatMap {
                        usersDao.updateUserRetryable(
                            txSession, it.get().copyBuilder()
                                .roles(emptyMap())
                                .build()
                        )
                    }
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.depriveRoleRetryable(
                    txSession,
                    3036L,
                )
            }
        }.block()

        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                usersDao.getById(txSession, TestUsers.SERVICE_1_QUOTA_MANAGER_2, Tenants.DEFAULT_TENANT_ID)
                    .flatMap {
                        usersDao.updateUserRetryable(
                            txSession, it.get().copyBuilder()
                                .roles(emptyMap())
                                .build()
                        )
                    }
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.depriveRoleRetryable(
                    txSession,
                    7331L,
                )
            }
        }.block()


        val mailsBeforeRefresh = mailSender.counter
        refreshTransferRequests.execute()
        val mailsAfterRefresh = mailSender.counter

        val getResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/transfers/{id}", createResult!!.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(getResult)
        val transfer = getResult!!.transfer
        Assertions.assertEquals(TransferRequestStatusDto.REJECTED, transfer.status)
        checkIsNotInPendingTransferRequest(transfer.id)
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_SERVICE_D,
            emptyList()
        )
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_2_ID,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER,
                TestUsers.SERVICE_1_QUOTA_MANAGER_2
            )
        )

        Assertions.assertEquals(
            listOf(
                TransferRequestHistoryEventTypeDto.REJECTED,
                TransferRequestHistoryEventTypeDto.CREATED
            ),
            getHistoryEventTypes(transfer)
        )

        val transferUrlWithName = "((https://abc.test.yandex-team.ru/folders/transfers/${createResult.transfer.id}" +
            " Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка))"
        val expectedUpdateTicketDto = TrackerUpdateTicketDto(
            "Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка",
            """
                Сервис-источник: https://abc.test.yandex-team.ru/services/dispenser
                Подтверждающие: staff:login-10, staff:login-12
                Сервис-получатель: https://abc.test.yandex-team.ru/services/d
                Подтверждающие:
                Заявка в ABCD: $transferUrlWithName

                **Провайдер: YP**
                YP-HDD-MAN: 1 GB

                Комментарий:
                test description
                """.trimIndent(),
            listOf(1L, transferComponentId),
            listOf("dispenser", "d")
        )
        val updateTicketDtoCaptor = ArgumentCaptor.forClass(TrackerUpdateTicketDto::class.java)
        Mockito.verify(trackerClient)
            .updateTicket(Mockito.eq(ticketKey), updateTicketDtoCaptor.capture())
        Assertions.assertEquals(1, updateTicketDtoCaptor.allValues.size)
        assertEqualsUpdateTicketDtoDespiteOrderOfResponsible(expectedUpdateTicketDto, updateTicketDtoCaptor.value)

        Mockito.verify(trackerClient).closeTicket(
            ticketKey,
            "close",
            TrackerTransitionExecuteDto(TrackerIssueResolution.WONT_FIX.toString())
        )
        Assertions.assertEquals(mailsBeforeRefresh, mailsAfterRefresh)
    }

    @Test
    fun `Test refresh can update many transfer requests`() {
        val ticketKeysIterator = listOf("DISPENSERTREQ-1", "DISPENSERTREQ-2").iterator()
        Mockito.`when`(
            trackerClient.createTicket(Mockito.any())
        ).thenReturn(
            Mono.just(Result.success(TrackerCreateTicketResponseDto(ticketKeysIterator.next())))
        )
        Mockito.`when`(
            trackerClient.updateTicket(Mockito.any(), Mockito.any())
        ).thenReturn(
            Mono.just(Result.failure(ErrorCollection.builder().build()))
        )
        val createResult1 = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(
                FrontCreateTransferRequestDto.builder()
                    .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                    .description("test description")
                    .addConfirmation(false)
                    .parameters(
                        FrontCreateTransferRequestParametersDto.builder()
                            .addQuotaTransfer(
                                FrontCreateQuotaTransferDto.builder()
                                    .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                    .destinationServiceId(TestServices.TEST_SERVICE_ID_D.toString())
                                    .addResourceTransfer(
                                        FrontCreateQuotaResourceTransferDto.builder()
                                            .delta("1")
                                            .resourceId(TestResources.YP_HDD_MAN)
                                            .deltaUnitId(UnitIds.GIGABYTES)
                                            .build()
                                    ).build()
                            )
                            .addQuotaTransfer(
                                FrontCreateQuotaTransferDto.builder()
                                    .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                    .destinationServiceId(TestServices.TEST_SERVICE_ID_DISPENSER.toString())
                                    .addResourceTransfer(
                                        FrontCreateQuotaResourceTransferDto.builder()
                                            .delta("-1")
                                            .resourceId(TestResources.YP_HDD_MAN)
                                            .deltaUnitId(UnitIds.GIGABYTES)
                                            .build()
                                    ).build()
                            ).build()
                    ).build()
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult1)

        val newQuotaManager = with(UserModel.builder()) {
            id(UUID.randomUUID().toString())
            tenantId(Tenants.DEFAULT_TENANT_ID)
            passportUid("1001")
            passportLogin("test-login-1001")
            staffId(1001L)
            staffDismissed(false)
            staffRobot(false)
            staffAffiliation(StaffAffiliation.YANDEX)
            roles(mapOf(UserServiceRoles.QUOTA_MANAGER to setOf(TestServices.TEST_SERVICE_ID_D)))
            gender("M")
            firstNameEn("test")
            firstNameRu("test")
            lastNameEn("test")
            lastNameRu("test")
            dAdmin(false)
            deleted(false)
            workEmail("login-1001@yandex-team.ru")
            langUi("ru")
            timeZone("Europe/Moscow")
        }.build()
        val newQuotaManagerAbcServiceMemberModel = with(AbcServiceMemberModel.newBuilder()) {
            id(1001)
            staffId(1001)
            serviceId(TestServices.TEST_SERVICE_ID_D)
            roleId(quotaManagerRoleId)
            state(AbcServiceMemberState.ACTIVE)
        }.build()

        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                usersDao.upsertUserRetryable(txSession, newQuotaManager)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.upsertManyRetryable(txSession, listOf(newQuotaManagerAbcServiceMemberModel))
            }
        }.block()

        val createResult2 = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(
                FrontCreateTransferRequestDto.builder()
                    .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                    .description("test description")
                    .addConfirmation(false)
                    .parameters(
                        FrontCreateTransferRequestParametersDto.builder()
                            .addQuotaTransfer(
                                FrontCreateQuotaTransferDto.builder()
                                    .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                    .destinationServiceId(TestServices.TEST_SERVICE_ID_D.toString())
                                    .addResourceTransfer(
                                        FrontCreateQuotaResourceTransferDto.builder()
                                            .delta("1")
                                            .resourceId(TestResources.YP_HDD_MAN)
                                            .deltaUnitId(UnitIds.GIGABYTES)
                                            .build()
                                    ).build()
                            ).addQuotaTransfer(
                                FrontCreateQuotaTransferDto.builder()
                                    .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                    .destinationServiceId(TestServices.TEST_SERVICE_ID_DISPENSER.toString())
                                    .addResourceTransfer(
                                        FrontCreateQuotaResourceTransferDto.builder()
                                            .delta("-1")
                                            .resourceId(TestResources.YP_HDD_MAN)
                                            .deltaUnitId(UnitIds.GIGABYTES)
                                            .build()
                                    ).build()
                            ).build()
                    ).build()
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult2)

        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                usersDao.getById(txSession, TestUsers.SERVICE_1_QUOTA_MANAGER, Tenants.DEFAULT_TENANT_ID)
                    .flatMap {
                        usersDao.updateUserRetryable(
                            txSession, it.get().copyBuilder()
                                .roles(emptyMap())
                                .build()
                        )
                    }
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.depriveRoleRetryable(
                    txSession,
                    3036L,
                )
            }
        }.block()

        refreshTransferRequests.execute()
    }

    @Test
    fun `Test refresh moves the responsible from the parent service to the child service`() {
        val ticketKey = "DISPENSERTREQ-1"
        Mockito.`when`(
            trackerClient.createTicket(Mockito.any())
        ).thenReturn(
            Mono.just(Result.success(TrackerCreateTicketResponseDto(ticketKey)))
        )
        Mockito.`when`(
            trackerClient.updateTicket(ArgumentMatchers.eq(ticketKey), Mockito.any())
        ).thenReturn(
            Mono.just(Result.failure(ErrorCollection.builder().build()))
        )
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.depriveRoleRetryable(txSession, 4001)
            }
        }.block()
        val request = FrontCreateTransferRequestDto.builder()
            .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
            .description("test description")
            .addConfirmation(false)
            .parameters(
                FrontCreateTransferRequestParametersDto.builder()
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_9_ID)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_IR.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    )
                    .addQuotaTransfer(
                        FrontCreateQuotaTransferDto.builder()
                            .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                            .destinationServiceId(TestServices.TEST_SERVICE_ID_DISPENSER.toString())
                            .addResourceTransfer(
                                FrontCreateQuotaResourceTransferDto.builder()
                                    .delta("-1")
                                    .resourceId(TestResources.YP_HDD_MAN)
                                    .deltaUnitId(UnitIds.GIGABYTES)
                                    .build()
                            ).build()
                    ).build()
            ).build()
        Long2LongMultimap().let { parents ->
            parents.resetAll(
                TestServices.TEST_SERVICE_ID_IR,
                LongOpenHashSet(setOf(TestServices.TEST_SERVICE_ID_MARKET))
            )
            tableClient.usingSessionMonoRetryable { session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                    servicesDao.upsertAllParentsRetryable(txSession, parents, Tenants.DEFAULT_TENANT_ID)
                }
            }.block()
        }
        val createResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .post()
            .uri("/front/transfers")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(createResult)

        val updatedQuotaManager = with(UserModel.builder()) {
            id(TestUsers.SERVICE_8_QUOTA_MANAGER)
            tenantId(Tenants.DEFAULT_TENANT_ID)
            passportUid("1120000000000014")
            passportLogin("login-14")
            staffId(14L)
            staffDismissed(false)
            staffRobot(false)
            staffAffiliation(StaffAffiliation.YANDEX)
            roles(mapOf(UserServiceRoles.QUOTA_MANAGER to setOf(TestServices.TEST_SERVICE_ID_IR)))
            gender("F")
            firstNameEn("test")
            firstNameRu("test")
            lastNameEn("test")
            lastNameRu("test")
            dAdmin(false)
            deleted(false)
            workEmail("login-14@yandex-team.ru")
            langUi("ru")
            timeZone("Europe/Moscow")
        }.build()
        val newQuotaManagerAbcServiceMemberModel = with(AbcServiceMemberModel.newBuilder()) {
            id(1001)
            staffId(14)
            serviceId(TestServices.TEST_SERVICE_ID_IR)
            roleId(quotaManagerRoleId)
            state(AbcServiceMemberState.ACTIVE)
        }.build()

        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                usersDao.updateUserRetryable(txSession, updatedQuotaManager)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.depriveRoleRetryable(txSession, 4000)
            }
        }.block()
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                abcServiceMemberDao.upsertManyRetryable(txSession, listOf(newQuotaManagerAbcServiceMemberModel))
            }
        }.block()

        val mailsBeforeRefresh = mailSender.counter
        refreshTransferRequests.execute()
        val mailsAfterRefresh = mailSender.counter

        val getResult = webClient
            .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
            .get()
            .uri("/front/transfers/{id}", createResult!!.transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontSingleTransferRequestDto::class.java)
            .returnResult()
            .responseBody
        Assertions.assertNotNull(getResult)
        val transfer = getResult!!.transfer
        Assertions.assertEquals(TransferRequestStatusDto.PENDING, transfer.status)
        checkIsInPendingTransferRequest(transfer.id)
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_9_ID,
            listOf(
                TestUsers.SERVICE_8_QUOTA_MANAGER,
            )
        )
        checkResponsibleIdsByFolder(
            transfer,
            TestFolders.TEST_FOLDER_2_ID,
            listOf(
                TestUsers.SERVICE_1_QUOTA_MANAGER,
                TestUsers.SERVICE_1_QUOTA_MANAGER_2
            )
        )

        Assertions.assertEquals(
            listOf(
                TransferRequestHistoryEventTypeDto.UPDATED,
                TransferRequestHistoryEventTypeDto.CREATED
            ),
            getHistoryEventTypes(transfer)
        )

        val transferUrlWithName = "((https://abc.test.yandex-team.ru/folders/transfers/${createResult.transfer.id}" +
            " Перенос квоты из dispenser:Проверочная папка в ir:Проверочная папка))"
        val expectedUpdateTicketDto = TrackerUpdateTicketDto(
            "Перенос квоты из dispenser:Проверочная папка в ir:Проверочная папка",
            """
                Сервис-источник: https://abc.test.yandex-team.ru/services/dispenser
                Подтверждающие: staff:login-10, staff:login-12
                Сервис-получатель: https://abc.test.yandex-team.ru/services/ir
                Подтверждающие: staff:login-14
                Заявка в ABCD: $transferUrlWithName

                **Провайдер: YP**
                YP-HDD-MAN: 1 GB

                Комментарий:
                test description
                """.trimIndent(),
            listOf(1L, transferComponentId),
            listOf("dispenser", "market", "ir")
        )
        val updateTicketDtoCaptor = ArgumentCaptor.forClass(TrackerUpdateTicketDto::class.java)
        Mockito.verify(trackerClient)
            .updateTicket(Mockito.eq(ticketKey), updateTicketDtoCaptor.capture())
        Assertions.assertEquals(1, updateTicketDtoCaptor.allValues.size)
        assertEqualsUpdateTicketDtoDespiteOrderOfResponsible(expectedUpdateTicketDto, updateTicketDtoCaptor.value)
        Mockito.verify(trackerClient, Mockito.never())
            .closeTicket(Mockito.any(), Mockito.any(), Mockito.any())
        Assertions.assertEquals(1, mailsAfterRefresh - mailsBeforeRefresh)
    }
}
