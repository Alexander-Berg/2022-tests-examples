package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.test.JdbcSpecTemplate

class ClientCommentsSpec extends AsyncWordSpec with JdbcSpecTemplate {

  val clientDao = new JdbcClientDao(office7Database, office7Database)

  "JdbcClientDao" should {
    "find comment for client" in {
      val comment = clientDao.getClientComment(1L)
      comment.futureValue shouldBe Some("Client 1 comment")
    }

    "not find non-existent comment" in {
      val comment = clientDao.getClientComment(2L).futureValue
      comment shouldBe None
    }
  }

}
