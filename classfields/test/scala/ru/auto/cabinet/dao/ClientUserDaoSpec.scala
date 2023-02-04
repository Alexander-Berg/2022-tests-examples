package ru.auto.cabinet.dao

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.dao.jdbc.ClientUserDao
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.test.JdbcSpecTemplate

class ClientUserDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {

  implicit val instr = new EmptyInstr("test")
  val dao = new ClientUserDao(office7Database, office7Database)

  val clientId = 20101

  "ClientUserDao" should {
    "find client users" in {
      for {
        users <- dao.usersByClientId(clientId)
      } yield users shouldBe Seq(1, 3)
    }
  }

}
