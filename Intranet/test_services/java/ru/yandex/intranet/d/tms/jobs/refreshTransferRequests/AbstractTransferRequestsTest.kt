package ru.yandex.intranet.d.tms.jobs.refreshTransferRequests

import com.yandex.ydb.table.transaction.TransactionMode
import org.junit.jupiter.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.TestUsers
import ru.yandex.intranet.d.dao.Tenants
import ru.yandex.intranet.d.dao.services.ServicesDao
import ru.yandex.intranet.d.dao.transfers.PendingTransferRequestsDao
import ru.yandex.intranet.d.dao.users.AbcServiceMemberDao
import ru.yandex.intranet.d.dao.users.UsersDao
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub
import ru.yandex.intranet.d.services.tracker.TrackerClient
import ru.yandex.intranet.d.tms.jobs.RefreshTransferRequests
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestDto
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestHistoryEventsPageDto

/**
 * Abstract test for cron job to refresh transfer requests.
 *
 * @author Petr Surkov <petrsurkov></petrsurkov>@yandex-team.ru>
 * @see [DISPENSER-4357](https://st.yandex-team.ru/DISPENSER-4357)
 */
abstract class AbstractTransferRequestsTest {

    @Value("\${abc.roles.quotaManager}")
    protected var quotaManagerRoleId: Long = 0

    @Value("\${tracker.transfer.type.transfer}")
    protected var transferComponentId: Long = 0

    @Value("\${tracker.transfer.type.reserve}")
    protected var transferReserveComponentId: Long = 0

    @Autowired
    protected lateinit var refreshTransferRequests: RefreshTransferRequests

    @Autowired
    protected lateinit var webClient: WebTestClient

    @Autowired
    protected lateinit var tableClient: YdbTableClient

    @Autowired
    protected lateinit var usersDao: UsersDao

    @Autowired
    protected lateinit var abcServiceMemberDao: AbcServiceMemberDao

    @Autowired
    protected lateinit var servicesDao: ServicesDao

    @Autowired
    protected lateinit var pendingTransferRequestsDao: PendingTransferRequestsDao

    @Autowired
    protected lateinit var mailSender: NotificationMailSenderStub

    @MockBean
    protected lateinit var trackerClient: TrackerClient

    protected fun checkResponsibleIdsByFolder(
        transfer: FrontTransferRequestDto,
        folderId: String,
        exceptedIds: List<String>
    ) {
        val matchingGroups = transfer.transferResponsible.responsibleGroups.filter {
            it.folders == listOf(folderId)
        }
        Assertions.assertEquals(1, matchingGroups.size)
        val actualIds = matchingGroups.first().responsibles.map { it.responsibleId }
        Assertions.assertEquals(exceptedIds.toSet(), actualIds.toSet())
        Assertions.assertEquals(exceptedIds.size, actualIds.size)
    }

    protected fun checkIsInPendingTransferRequest(id: String) =
        Assertions.assertTrue(getPendingTransferRequest(id).isPresent)

    protected fun checkIsNotInPendingTransferRequest(id: String) =
        Assertions.assertTrue(getPendingTransferRequest(id).isEmpty)

    protected fun getHistoryEventTypes(transfer: FrontTransferRequestDto) =
        webClient
            .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
            .get()
            .uri("/front/transfers/{id}/history", transfer.id)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontTransferRequestHistoryEventsPageDto::class.java)
            .returnResult()
            .responseBody!!
            .events
            .map { it.eventType }

    private fun getPendingTransferRequest(id: String) =
        tableClient.usingSessionMonoRetryable { session ->
            session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE) { txSession ->
                pendingTransferRequestsDao.getById(txSession, id, Tenants.DEFAULT_TENANT_ID)
            }
        }.block()!!
}
