package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import org.scalatest.RecoverMethods.recoverToExceptionIf
import ru.yandex.vertis.telepony.dao.WhitelistHistoryDao.{DeleteWhitelistRequest, ForbiddenOwnerException, ListActualWhitelistRequest}
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{AntiFraudOptions, TypedDomains}
import ru.yandex.vertis.telepony.service.WhitelistHistoryService.ActualWhitelistNumber
import ru.yandex.vertis.telepony.util.Page
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.concurrent.duration._

class WhitelistHistoryServiceImplIntSpec extends SpecBase with IntegrationSpecTemplate with BeforeAndAfterEach {

  import materializer.executionContext

  private lazy val whitelistService = sharedWhitelistHistoryService

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    whitelistHistoryDao.clear().futureValue
  }

  "WhitelistService" should {
    "add new numbers" in {
      val request = AddOrUpdateWhitelistRequestGen.next.copy(
        domains = Seq(TypedDomains.autoru_def),
        allDomainsFlag = false
      )
      whitelistService.addOrUpdate(request).futureValue

      val actualList = whitelistService.listAll().futureValue

      actualList.size shouldBe request.sources.size
      actualList.map(_.source) should contain theSameElementsAs request.sources
      actualList.forall(_.domain == TypedDomains.autoru_def) shouldBe true
      actualList.forall(_.ownerId == request.ownerId) shouldBe true
      actualList.forall(_.comment == request.comment) shouldBe true
    }

    "add to all domains" in {
      val request = AddOrUpdateWhitelistRequestGen.next.copy(
        allDomainsFlag = true
      )
      whitelistService.addOrUpdate(request).futureValue

      val actualList = whitelistService.listAll().futureValue
      actualList.size shouldBe (request.sources.size * TypedDomains.ActualDomains.size)
    }

    "throw exception on try update with another ownerId" in {
      val owner1 = "owner-1"
      val owner2 = "owner-2"

      val request1 = AddOrUpdateWhitelistRequestGen.next.copy(
        ownerId = owner1
      )
      val request2 = request1.copy(
        ownerId = owner2
      )

      whitelistService.addOrUpdate(request1).futureValue

      recoverToExceptionIf[ForbiddenOwnerException] {
        whitelistService.addOrUpdate(request2)
      }.futureValue
    }

    "delete number as setting end time" in {
      val request = AddOrUpdateWhitelistRequestGen.next.copy(
        ttl = 1.hour
      )

      whitelistService.addOrUpdate(request).futureValue

      val actualList1 = whitelistService.listAll().futureValue
      actualList1.forall(_.endTime.isAfterNow) shouldBe true

      whitelistService
        .delete(
          DeleteWhitelistRequest(
            ownerId = request.ownerId,
            sources = request.sources,
            domains = request.domains,
            allDomainsFlag = request.allDomainsFlag
          )
        )
        .futureValue

      val actualList2 = whitelistService.listAll().futureValue
      actualList2.forall(_.endTime.isBeforeNow) shouldBe true
    }

    "list actual number" in {
      val addOrUpdateRequest = AddOrUpdateWhitelistRequestGen.next.copy(allDomainsFlag = true)
      whitelistService.addOrUpdate(addOrUpdateRequest).futureValue

      val domain = TypedDomains.autoru_def
      val endTime = whitelistService.listAll().futureValue.head.endTime

      val listRequest = ListActualWhitelistRequest(
        ownerId = addOrUpdateRequest.ownerId,
        domains = Seq(domain),
        allDomainsFlag = false,
        slice = Page.Default
      )

      val expectedList = addOrUpdateRequest.sources.map { source =>
        ActualWhitelistNumber(
          source = source,
          domain = domain,
          comment = addOrUpdateRequest.comment,
          endTime = endTime
        )
      }

      whitelistService.list(listRequest).futureValue should contain theSameElementsAs expectedList
    }

    "get actual from" in {
      val addOrUpdateRequest = AddOrUpdateWhitelistRequestGen.next.copy(allDomainsFlag = false)
      val from = DateTime.now().plus(addOrUpdateRequest.ttl.toMillis.toInt / 3)
      whitelistService.addOrUpdate(addOrUpdateRequest).futureValue

      addOrUpdateRequest.domains.foreach { domain =>
        addOrUpdateRequest.sources.foreach { source =>
          val actual = whitelistService.getStillActual(source, domain, from).futureValue
          actual.map(_.ownerId) shouldBe Some(addOrUpdateRequest.ownerId)
          actual.map(_.comment) shouldBe Some(addOrUpdateRequest.comment)
        }
      }
    }
  }
}
