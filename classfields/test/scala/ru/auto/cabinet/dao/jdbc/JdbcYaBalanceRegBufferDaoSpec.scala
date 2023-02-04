package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.test.JdbcSpecTemplate

class JdbcYaBalanceRegBufferDaoSpec
    extends AsyncWordSpec
    with JdbcSpecTemplate {

  private val balanceDao =
    new JdbcYaBalanceRegBufferDao(balanceDatabase, balanceDatabase)

  "YaBalanceRegistrationDao" should {

    "correctly query unprocessed clients" in {
      for {
        res <- balanceDao.getUnprocessedClients
        resIds = res.map(_.clientId)
      } yield {
        res should have size 2
        resIds should contain theSameElementsAs List(20101L, 20102L)
      }
    }

    "correctly add clients as unprocessed" in {
      for {
        prior <- balanceDao.getUnprocessedClients
        newClients = List(20105L, 20106L, 20107L)
        _ <- balanceDao.addClients(newClients)
        posterior <- balanceDao.getUnprocessedClients
      } yield posterior should have size (prior.length + 3)
    }

    "correctly mark elements as processed" in {
      for {
        prior <- balanceDao.getUnprocessedClients
        idToMark = prior.head.clientId
        _ <- balanceDao.markProcessed(idToMark)
        res1 <- balanceDao.getUnprocessedClients
        idToMark2 = res1.head.clientId
        _ <- balanceDao.markProcessed(idToMark2)
        res <- balanceDao.getUnprocessedClients
      } yield {
        res1 should have size (prior.size - 1)
        res should have size (prior.size - 2)
      }
    }

  }
}
