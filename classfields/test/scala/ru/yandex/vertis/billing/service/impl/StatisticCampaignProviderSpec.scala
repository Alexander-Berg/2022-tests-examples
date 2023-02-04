package ru.yandex.vertis.billing.service.impl

import org.mockito.Mockito.{reset, verify, verifyNoInteractions}
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.Uid
import ru.yandex.vertis.billing.model_core.gens.{CampaignHeaderGen, Producer}
import ru.yandex.vertis.billing.service.ArchiveService.{ArchiveRecord, CampaignPayload, RecordTypes}
import ru.yandex.vertis.billing.service.impl.StatisticCampaignProviderSpec._
import ru.yandex.vertis.billing.service.{ArchiveService, CampaignService}
import ru.yandex.vertis.billing.util.OperatorContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.annotation.nowarn
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Specs on [[StatisticCampaignProvider]]
  *
  * @author alex-kovalenko
  */
@nowarn("msg=discarded non-Unit value")
class StatisticCampaignProviderSpec extends AnyWordSpec with Matchers with Setup with TryValues with MockitoSupport {

  implicit val oc = OperatorContext("test", Uid(0L))

  implicit final class ExpectSuccess[A](t: Try[A]) {

    def expect(expected: A)(f: => Unit) =
      success { case `expected` =>
        f
      }

    def success(f: A => Unit) = t match {
      case Success(data) => f(data)
      case other => fail(s"Unexpected $other")
    }
  }

  "StatisticCampaignProvider if useArchive = true" should {
    "get campaign by id" when {
      "found in regular storage" in {
        when(campaigns.get(TestCampaignId))
          .thenReturn(Success(TestCampaign))

        provider.get(TestCampaignId).expect(TestCampaign) {
          verifyNoInteractions(archive)
        }
      }
    }

    "get campaign by id and customer" when {
      "found in regular storage" in {
        when(campaigns.get(TestCustomerId, TestCampaignId))
          .thenReturn(Success(TestCampaign))

        provider.get(TestCustomerId, TestCampaignId).expect(TestCampaign) {
          verifyNoInteractions(archive)
        }
      }

      "found in archive" in {
        when(campaigns.get(TestCustomerId, TestCampaignId))
          .thenReturn(Failure(new NoSuchElementException))
        when(archive.get(ArchiveForCustomerCampaign))
          .thenReturn(Success(Iterable(TestCampaignRecord)))

        provider.get(TestCustomerId, TestCampaignId).expect(TestCampaign) {
          verify(campaigns).get(TestCustomerId, TestCampaignId)
        }
      }
    }

    "get all customer's campaigns" when {
      val otherCampaign = CampaignHeaderGen.suchThat(_.id != TestCampaignId).next
      val allCampaigns = Iterable(TestCampaign, otherCampaign)

      def testWhen(desc: String)(setup: => Unit): Unit = {
        desc in {
          setup
          provider.get(TestCustomerId).success { result =>
            result should (have size allCampaigns.size
              and contain theSameElementsAs allCampaigns)
          }
        }
      }

      testWhen("all are regular") {
        when(campaigns.get(CampaignForCustomer))
          .thenReturn(Success(allCampaigns))
        when(archive.get(ArchiveForCustomer))
          .thenReturn(Success(Iterable.empty))
      }

      testWhen("campaigns are from different storages") {
        when(campaigns.get(CampaignForCustomer))
          .thenReturn(Success(Iterable(otherCampaign)))
        when(archive.get(ArchiveForCustomer))
          .thenReturn(Success(Iterable(TestCampaignRecord)))
      }

      testWhen("all are archived") {
        when(campaigns.get(CampaignForCustomer))
          .thenReturn(Success(Iterable.empty))
        when(archive.get(ArchiveForCustomer))
          .thenReturn(Success(allCampaigns.map(c => ArchiveRecord(TestCustomerId, "-", CampaignPayload(c)))))
      }
    }
  }

}

object StatisticCampaignProviderSpec {

  import ru.yandex.vertis.mockito.MockitoSupport.mock

  val TestCampaign = CampaignHeaderGen.next
  val TestCampaignId = TestCampaign.id
  val TestCustomerId = TestCampaign.customer.id

  val ArchiveForCustomer = ArchiveService.Filter.ForCustomer(RecordTypes.Campaign, TestCustomerId)

  val ArchiveForCustomerCampaign =
    ArchiveService.Filter.ForCustomerCampaign(RecordTypes.Campaign, TestCustomerId, TestCampaignId)

  val CampaignForCustomer = CampaignService.Filter.ForCustomer(TestCustomerId)

  val TestCampaignRecord = ArchiveRecord(TestCustomerId, "-", CampaignPayload(TestCampaign))

  trait Setup extends BeforeAndAfterEach {
    this: Suite =>

    val campaigns = mock[CampaignService]
    val archive = mock[ArchiveService]

    val provider = new StatisticCampaignProvider(campaigns, Some(archive))

    override def beforeEach(): Unit = {
      super.beforeEach()
      reset[Any](campaigns, archive)
    }
  }
}
