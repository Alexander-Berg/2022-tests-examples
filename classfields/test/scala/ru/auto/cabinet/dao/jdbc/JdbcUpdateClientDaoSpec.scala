package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.model.{BalanceClientEvents, ClientUpdate}
import ru.auto.cabinet.test.JdbcSpecTemplate
import ru.auto.cabinet.test.dao.jdbc.JdbcBalanceClientsChangedBufferDao
import ru.auto.cabinet.test.model.BalanceClientsChangedBufferRecord

class JdbcUpdateClientDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {
  private val clientDao = new JdbcClientDao(office7Database, office7Database)
  private val balanceDao = new BalanceDao(balanceDatabase, balanceDatabase)

  private val balanceClientsChangedBufferDao =
    new JdbcBalanceClientsChangedBufferDao(balanceDatabase, balanceDatabase)
  private val managerDao = new ManagerDao(crmDatabase, crmDatabase)

  private val clientsChangedBufferDao =
    new JdbcClientsChangedBufferDao(office7Database, office7Database)

  private val updateClientDao =
    new JdbcUpdateClientDao(
      office7Database,
      office7Database,
      balanceHandle.databaseName,
      crmHandle.databaseName
    )

  "JdbcUpdateClientDao" when {
    val agencyId = 201L
    val balanceAgencyId = 2001L
    val clientId = 105L
    val modifiedManagerEmail = "new@auto.yandex.ru"

    "agency is set" should {
      "update agency in office7, update agency in balance and add record in changed buffer" in {
        val clientUpdate =
          ClientUpdate(
            clientId,
            Some(agencyId),
            companyId = None,
            responsibleManagerEmail = None,
            comment = None,
            modifiedManagerEmail = None)

        for {
          changesBefore <- balanceClientsChangedBufferDao.get()
          _ <- updateClientDao.update(clientUpdate)
          clientOffice7 <- clientDao.get(clientId)
          clientBalance <- balanceDao.getBalanceClient(clientId)(
            operateOnMaster = false)
          changesAfter <- balanceClientsChangedBufferDao.get()
        } yield {
          clientOffice7.agencyId should be(agencyId)
          clientBalance.balanceAgencyId should be(Some(balanceAgencyId))
          changesAfter.size shouldBe changesBefore.size + 1
          changesAfter.find(_.clientId == clientId) shouldBe Some(
            BalanceClientsChangedBufferRecord(
              clientId,
              BalanceClientEvents.AgencyUpdate.entryName))
        }
      }
    }

    "agency is not set" should {
      "delete agency in office7, delete agency in balance and add new record in changed buffer" in {
        val clientUpdate = ClientUpdate(
          clientId,
          agencyId = None,
          companyId = None,
          responsibleManagerEmail = None,
          comment = None,
          modifiedManagerEmail = None)

        for {
          changesBefore <- balanceClientsChangedBufferDao.get()
          _ <- updateClientDao.update(clientUpdate)
          clientOffice7 <- clientDao.get(clientId)
          clientBalance <- balanceDao.getBalanceClient(clientId)(
            operateOnMaster = false)
          changesAfter <- balanceClientsChangedBufferDao.get()
        } yield {
          clientOffice7.agencyId should be(0)
          clientBalance.balanceAgencyId should be(Some(0))
          changesAfter.size shouldBe changesBefore.size + 1
        }
      }
    }

    "responsible manager is set" should {
      "set responsible manger in office7 and crm" in {
        val responsibleManagerEmail = "new@auto.yandex.ru"
        val clientUpdate = ClientUpdate(
          clientId,
          agencyId = None,
          companyId = None,
          Some(responsibleManagerEmail),
          comment = None,
          modifiedManagerEmail = None)

        val expectedManager =
          ManagerRecord(
            id = 6,
            email = Some(
              responsibleManagerEmail
                .replace("auto.yandex.ru", "yandex-team.ru")))

        for {
          _ <- updateClientDao.update(clientUpdate)
          clientOffice7 <- clientDao.get(clientId)
          crmManager <- managerDao.getManager(clientId)
        } yield {
          clientOffice7.properties.responsibleManagerEmail should be(
            Some(responsibleManagerEmail))
          crmManager should be(Some(expectedManager))
        }
      }
    }

    "responsible manager is not set" should {
      "not delete responsible manger in office7 and crm" in {
        val clientUpdate = ClientUpdate(
          clientId,
          agencyId = None,
          companyId = None,
          responsibleManagerEmail = None,
          comment = None,
          modifiedManagerEmail = None)

        for {
          _ <- updateClientDao.update(clientUpdate)
          clientOffice7 <- clientDao.get(clientId)
          crmManager <- managerDao.getManager(clientId)
        } yield {
          clientOffice7.properties.responsibleManagerEmail shouldNot be(None)
          crmManager shouldNot be(None)
        }
      }
    }

    "every kind of update" should {
      "add client to changed buffer" in {
        import org.scalatest.prop.TableDrivenPropertyChecks._

        val responsibleManager = "new@auto.yandex.ru"
        val comment = "comment for test update"
        val companyId = -30L

        val updates = Table(
          "update",
          ClientUpdate(clientId, None, None, None, None, None),
          ClientUpdate(clientId, Some(agencyId), None, None, None, None),
          ClientUpdate(clientId, None, Some(companyId), None, None, None),
          ClientUpdate(
            clientId,
            None,
            None,
            Some(responsibleManager),
            None,
            None),
          ClientUpdate(clientId, None, None, None, Some(comment), None),
          ClientUpdate(
            clientId,
            None,
            None,
            None,
            None,
            Some(modifiedManagerEmail))
        )
        forAll(updates) { clientUpdate =>
          for {
            before <- clientsChangedBufferDao.get()
            _ <- updateClientDao.update(clientUpdate)
            after <- clientsChangedBufferDao.get()
          } yield {
            val changedClients = after.toSet -- before
            import org.scalatest.LoneElement._
            val updatedClient = changedClients.loneElement
            updatedClient.clientId should be(clientId)
            updatedClient.dataSource should be("amoCRM")
          }
        }
      }
    }

    "comment is provided" should {
      "update comment" in {
        val comment = "comment for test update"

        val clientUpdate =
          ClientUpdate(
            clientId = clientId,
            agencyId = None,
            companyId = None,
            responsibleManagerEmail = None,
            comment = Some(comment),
            modifiedManagerEmail = Some(modifiedManagerEmail))

        for {
          _ <- updateClientDao.update(clientUpdate)
          result <- clientDao.getClientComment(clientId)
        } yield result should be(Some(comment))
      }
    }

    "updateAgency" should {
      "update agency id successfully" in {
        for {
          _ <- updateClientDao.updateAgency(clientId, agencyId)
          balanceClient <- balanceDao
            .getBalanceClient(clientId)(operateOnMaster = false)
          client <- clientDao.get(clientId)
        } yield {
          balanceClient.balanceAgencyId should be(Some(balanceAgencyId))
          client.agencyId should be(agencyId)
        }
      }
    }
  }
}
