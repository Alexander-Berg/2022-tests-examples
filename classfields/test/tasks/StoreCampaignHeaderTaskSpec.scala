package ru.yandex.vertis.billing.tasks

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.{CampaignHistoryPoint, EventType, EventTypes, Filter}
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao
import ru.yandex.vertis.billing.dao.impl.jdbc.{
  JdbcCampaignDao,
  JdbcCampaignHistoryDao,
  JdbcCustomerDao,
  JdbcKeyValueDao,
  JdbcSpecTemplate
}
import ru.yandex.vertis.billing.model_core.gens.{CustomerHeaderGen, OrderPropertiesGen, Producer, ProductGen}
import ru.yandex.vertis.billing.model_core.{CampaignHeader, CampaignId, CampaignSettings}
import ru.yandex.vertis.billing.service.CampaignService
import ru.yandex.vertis.billing.service.CampaignService.Patch
import ru.yandex.vertis.billing.service.impl.EpochServiceImpl
import ru.yandex.vertis.billing.tasks.StoreCampaignHeaderTaskSpec.EpochMarker
import ru.yandex.vertis.billing.util.clean.CleanableCampaignHistoryDao
import ru.yandex.vertis.billing.util.{DateTimeInterval, DateTimeUtils}

import scala.util.Success

/**
  * Spec on [[StoreCampaignHeaderTask]]
  *
  * @author ruslansd
  */
class StoreCampaignHeaderTaskSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate with BeforeAndAfterEach {

  private val campaignDao = new JdbcCampaignDao(billingDatabase)
  private val customerDao = new JdbcCustomerDao(billingDatabase)
  private val orderDao = new JdbcOrderDao(billingDualDatabase)

  private val epochService = {
    val kv = new JdbcKeyValueDao(billingDatabase)
    new EpochServiceImpl(kv)
  }
  private val history = new JdbcCampaignHistoryDao(billingDatabase) with CleanableCampaignHistoryDao

  override def beforeEach(): Unit = {
    super.beforeEach()
    history.clean().get
  }

  val from: DateTime = DateTimeUtils.fromMillis(0L)
  def to: DateTime = DateTimeUtils.now()
  def interval: DateTimeInterval = DateTimeInterval(from, to)

  private def createCampaign(id: CampaignId): CampaignHeader = {
    val customer = CustomerHeaderGen.next
    customerDao.create(customer).get
    val orderProperties = OrderPropertiesGen.next
    val order = orderDao.create(customer.id, orderProperties).get
    val product = ProductGen.next
    val settings = CampaignSettings.Default
    val source = CampaignService.Source(Some("foo"), order.id, product, settings, Some(id), Iterable.empty)
    campaignDao.create(customer.id, source, DuplicationPolicy.AllowDuplicates).get
  }

  private def toHistoryPoint(header: CampaignHeader, eventType: EventType): CampaignHistoryPoint = {
    val headerWithoutCreateTimeAndStatus = header.copy(createTimestamp = None, status = None)
    CampaignHistoryPoint(headerWithoutCreateTimeAndStatus, eventType)
  }

  private val task = new StoreCampaignHeaderTask(campaignDao, history, epochService)

  "StoreCampaignHeaderTask" should {
    "get nothing" when {
      "task work on empty set" in {
        checkTask()
        history.getTry(Filter.InIntervalAndLastBefore(interval)) shouldBe Success(Iterable.empty)
        history.getTry(Filter.UpdatedSinceBatchOrdered(from.getMillis, None, 100)) shouldBe Success(Iterable.empty)
      }
    }
    "work with created campaigns" in {
      val headers = (0 to 1).map { i =>
        createCampaign(i.toString)
      }
      checkTask()

      history.getTry(Filter.InIntervalAndLastBefore(interval)) match {
        case Success(hdrs) =>
          val expectedResult = headers.map(toHistoryPoint(_, EventTypes.Create))
          hdrs should contain theSameElementsAs expectedResult
          epochService.getTry(EpochMarker).get shouldBe headers.flatMap(_.epoch).max
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "correctly work on campaign update" in {
      val headers = campaignDao.get(CampaignService.Filter.All).get
      val updatedHeaders = headers.map { h =>
        campaignDao
          .update(h.customer.id, h.id, Patch(product = Some(ProductGen.next)), DuplicationPolicy.AllowDuplicates)
          .get
      }
      checkTask()
      val expected = updatedHeaders.map(toHistoryPoint(_, EventTypes.Update))
      history.getTry(Filter.InIntervalAndLastBefore(interval)) match {
        case Success(hdrs) =>
          hdrs should contain theSameElementsAs expected
          epochService.getTry(EpochMarker).get shouldBe updatedHeaders.flatMap(_.epoch).max
        case other =>
          fail(s"Unexpected $other")
      }
    }

  }

  def checkTask(): Unit =
    task.execute() match {
      case Success(_) =>
      case other =>
        fail(s"Unexpected $other")
    }

}

object StoreCampaignHeaderTaskSpec {
  private val EpochMarker = "campaign_history"
}
