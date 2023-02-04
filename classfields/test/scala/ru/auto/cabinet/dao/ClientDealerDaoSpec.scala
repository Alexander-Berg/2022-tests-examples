package ru.auto.cabinet.dao

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.dao.jdbc.ClientDealerDao
import ru.auto.cabinet.model.ClientMarks
import ru.auto.cabinet.test.JdbcSpecTemplate

class ClientDealerDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {

  private val dao = new ClientDealerDao(office7Database, office7Database)

  val clientId = 20101
  val clientMarks = ClientMarks(clientId, Seq(1, 3))

  "ClientDealerDaoSpec" should {
    "find client marks" in {
      for {
        record <- dao.findClientMarks(clientId)
      } yield record shouldBe clientMarks
    }
  }

}
