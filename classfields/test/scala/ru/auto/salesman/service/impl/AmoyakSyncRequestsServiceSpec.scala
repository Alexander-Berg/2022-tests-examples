package ru.auto.salesman.service.impl

import java.util

import ru.auto.amoyak.InternalServiceModel.AmoSyncRequest
import ru.auto.salesman.dao.ClientsChangedBufferDao
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.AutomatedContext

class AmoyakSyncRequestsServiceSpec extends BaseSpec {

  val clientsChangedBufferDaoMock: ClientsChangedBufferDao =
    mock[ClientsChangedBufferDao]

  val amoyakSyncRequestsService = new AmoyakSyncRequestsServiceImpl(
    clientsChangedBufferDaoMock
  )

  implicit val rc: AutomatedContext = AutomatedContext("test")

  "create" should {
    "create sync requests for given clients" in {
      val dataSource = "sync"
      val amoSyncRequest =
        AmoSyncRequest
          .newBuilder()
          .addAllClientIds(util.Arrays.asList(1L, 2L, 3L))
          .build()

      val clientInputRecords = List(
        ClientsChangedBufferDao.InputRecord(1, dataSource),
        ClientsChangedBufferDao.InputRecord(2, dataSource),
        ClientsChangedBufferDao.InputRecord(3, dataSource)
      )

      (clientsChangedBufferDaoMock.insert _)
        .expects(clientInputRecords)
        .returningZ(())

      amoyakSyncRequestsService.create(amoSyncRequest).success.value should be(
        ()
      )
    }
  }
}
