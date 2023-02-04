package ru.auto.cabinet.dao.jdbc

import java.time.{OffsetDateTime, ZoneOffset}

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.dao.entities
import ru.auto.cabinet.dao.entities.{BalanceClient, ClientBalanceIds}
import ru.auto.cabinet.test.JdbcSpecTemplate

class JdbcBalanceDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {

  private val balanceDao = new BalanceDao(balanceHandle.db, balanceHandle.db)

  "BalanceDao" should {

    "find balance client by BalanceClientId" in {
      for {
        balanceClient <- balanceDao.getClient(2)(false)
      } yield {
        val expected = BalanceClient(
          id = 2,
          balanceClientId = Some(1002),
          balanceAgencyId = Some(102),
          regionId = None,
          name = None,
          contractId = None,
          clientType = None,
          email = None,
          phone = None,
          url = None,
          isAgency = Some(false)
        )

        balanceClient shouldBe expected
      }
    }

    "return failure when balance client by BalanceClientId not found" in {
      balanceDao.getClient(42)(false).failed.map { e =>
        e shouldBe a[BalanceClientIdNotFound]
      }
    }

    "find linked balance ids by office7 id" in {

      for {
        balanceIds <- balanceDao.getClientBalanceIdsByOffice7(
          List(10L, 20L, 30L))
      } yield {
        val expectedSeq = Seq(
          ClientBalanceIds(
            office7ClientId = 10L,
            balanceAgencyId = Some(101L),
            balanceClientId = Some(1001L),
            isAgency = Some(true)),
          ClientBalanceIds(
            office7ClientId = 20L,
            balanceAgencyId = Some(102L),
            balanceClientId = Some(1002L),
            isAgency = Some(false))
        )
        balanceIds.size shouldBe 2

        balanceIds should contain theSameElementsAs expectedSeq
      }
    }

    "find linked office7 clients by balance ids" in {
      for {
        ids <- balanceDao.getClientBalanceIdsByBalance(
          List(1001L, 1002L, 1003L))
      } yield {
        val expectedSeq = Seq(
          ClientBalanceIds(
            office7ClientId = 10L,
            balanceAgencyId = Some(101L),
            balanceClientId = Some(1001L),
            isAgency = Some(true)),
          ClientBalanceIds(
            office7ClientId = 20L,
            balanceAgencyId = Some(102L),
            balanceClientId = Some(1002L),
            isAgency = Some(false))
        )
        ids.size shouldBe 2

        ids should contain theSameElementsAs expectedSeq
      }
    }

    "find invoice requests" in {
      for {
        req <- balanceDao.getBalanceRequestByAutoruId(10L)
      } yield {
        val date =
          OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(3))
        val expectedSeq = Seq(
          entities.BalanceRequest(
            id = 54321,
            requestId = 4321,
            clientId = 1,
            agencyId = Some(0),
            createDate = Some(date),
            url = Some("url_1"),
            userId = Some(321),
            totalSum = Some(654321)
          )
        )
        req should contain theSameElementsAs expectedSeq

      }
    }

    "udpate balance_client_id with new id" in {
      val balanceId = 1L
      val updatedBalanceClientId = Some(10001L)
      for {
        clientOriginal <- balanceDao.getClient(balanceId)()
        _ <- balanceDao.updateBalanceClientId(balanceId, updatedBalanceClientId)
        clientUpdated <- balanceDao.getClient(balanceId)()
        _ <- balanceDao.updateBalanceClientId(clientOriginal.id, None)
        clientEmptyId <- balanceDao.getClient(balanceId)()
        _ <- balanceDao.updateBalanceClientId(
          clientOriginal.id,
          clientOriginal.balanceClientId)
        clientAfter <- balanceDao.getClient(balanceId)()
      } yield {
        clientUpdated shouldBe clientOriginal.copy(balanceClientId =
          updatedBalanceClientId)
        clientEmptyId shouldBe clientOriginal.copy(balanceClientId = None)
        clientAfter shouldBe clientOriginal
      }
    }
  }
}
