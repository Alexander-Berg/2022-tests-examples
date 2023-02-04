package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.test.JdbcSpecTemplate

class JdbcClientsChangedBufferDaoSpec
    extends AsyncWordSpec
    with JdbcSpecTemplate {

  private val clientsChangedBufferDao =
    new JdbcClientsChangedBufferDao(office7Database, office7Database)

  "ClientsChangedDao" should {
    "get clients" in {
      for {
        clients <- clientsChangedBufferDao.get()
      } yield clients.groupBy(_.clientId) should have size 3
    }

    "add clients" in {
      import ClientsChangedBufferDao.InputRecord

      val client1Id = 1L
      val client2Id = 4L
      val inputRecords =
        Seq(
          InputRecord(client1Id, "client_properties"),
          InputRecord(client2Id, "clients")
        )
      for {
        _ <- clientsChangedBufferDao.add(inputRecords)
        clients <- clientsChangedBufferDao.get()
        grouped = clients.groupBy(_.clientId)
        client1DataSources = grouped.get(client1Id)
        client2DataSources = grouped.get(client2Id)
      } yield {
        grouped should have size 4
        client1DataSources should not be None
        client1DataSources.get.map(_.dataSource) shouldBe Seq(
          "clients",
          "client_properties")
        client2DataSources should not be None
        client2DataSources.get.map(_.dataSource) shouldBe Seq("clients")
      }
    }

    "delete records" in {
      val recordIdToDelete = 4
      for {
        _ <- clientsChangedBufferDao.delete(recordIdToDelete)
        clients <- clientsChangedBufferDao.get()
        clientIds = clients.map(_.clientId)
      } yield clientIds shouldBe Seq(1, 2, 3, 4)
    }

    "not delete non-existent record" in {
      val recordIdToDelete = 6
      for {
        clients <- clientsChangedBufferDao.get()
        _ <- clientsChangedBufferDao.delete(recordIdToDelete)
        clientsAfterDelete <- clientsChangedBufferDao.get()
      } yield clients shouldBe clientsAfterDelete
    }
  }

}
