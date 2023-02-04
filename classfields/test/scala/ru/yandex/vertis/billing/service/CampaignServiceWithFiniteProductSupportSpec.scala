package ru.yandex.vertis.billing.service

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.gens.FiniteProductActionGen
import ru.yandex.vertis.billing.dao.impl.jdbc.{
  JdbcBindingDao,
  JdbcCampaignCallDao,
  JdbcFiniteProductDao,
  JdbcSpecTemplate
}
import ru.yandex.vertis.billing.dao.{CampaignDao, FiniteProductDao, TransactionContext}
import ru.yandex.vertis.billing.model_core.gens.{
  CampaignHeaderGen,
  CustomerIdGen,
  OrderIdGen,
  Producer,
  ProductGen,
  ProductWithDurationGen
}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.impl.CampaignServiceImpl
import ru.yandex.vertis.billing.util.AutomatedContext
import ru.yandex.vertis.billing.util.DateTimeUtils.DateTimeWithDuration
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

/**
  * Spec on [[CampaignServiceWithFiniteProductSupport]].
  *
  * @author ruslansd
  */
class CampaignServiceWithFiniteProductSupportSpec
  extends AnyWordSpec
  with Matchers
  with JdbcSpecTemplate
  with MockitoSupport {

  private val campaignCallDao = new JdbcCampaignCallDao(campaignEventDatabase)
  private val bindingDao = new JdbcBindingDao(billingDatabase)
  private val campaigns = collection.mutable.HashMap.empty[CampaignId, CampaignHeader]

  implicit private val ac = AutomatedContext("CampaignServiceWithFiniteProductSupportSpec")

  private val orderId = OrderIdGen.next
  private val customerId = CustomerIdGen.next

  private val campaignMock = {
    val m = mock[CampaignDao]

    stub(m.create(_: CustomerId, _: CampaignService.Source, _: CampaignDao.DuplicationPolicy)(_: TransactionContext)) {
      case (_, source, _, _) =>
        val header = CampaignHeaderGen.next
          .copy(product = source.product, settings = source.settings)
        campaigns += header.id -> header
        Success(header)
    }

    stub(
      m.update(_: CustomerId, _: CampaignId, _: CampaignService.Patch, _: CampaignDao.DuplicationPolicy)(
        _: TransactionContext
      )
    ) { case (_, id, patch, _, _) =>
      val header = campaigns(id)
      val updated = {
        patch.product match {
          case Some(p) =>
            header.copy(product = p)
          case _ =>
            header
        }
      }
      campaigns += updated.id -> updated
      Success(updated)
    }
    m
  }
  private val finiteProductDao = new JdbcFiniteProductDao(billingDatabase)

  private val service =
    new CampaignServiceImpl(campaignMock, bindingDao, campaignCallDao, DuplicationPolicy.AllowDuplicates)
      with CampaignServiceWithFiniteProductSupport {
      override def dao: FiniteProductDao = finiteProductDao
    }

  private def source(product: Product, action: Option[FiniteProductDao.Action] = None) =
    CampaignService.Source(
      None,
      orderId,
      product,
      CampaignSettings.Default,
      None,
      Iterable.empty,
      action = action
    )

  private def createCampaign(product: Product, action: Option[FiniteProductDao.Action]): CampaignHeader = {
    val s = source(product, action)
    service.create(customerId, s).get
  }

  "CampaignServiceWithFiniteProductSupport" should {
    "do nothing on non expire product" in {
      val product = ProductGen.next
      val header = service.create(customerId, source(product)).get

      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.headOption shouldBe None
    }

    "insert record when finite product" in {
      val product = ProductWithDurationGen.next
      val action = FiniteProductActionGen.next
      val header = createCampaign(product, Some(action))

      val expectedRecord = FiniteProductDao.FiniteProductRecord(
        header.id,
        product.from.get,
        product.from.get.plus(product.duration.get),
        header.product,
        action
      )
      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.head shouldBe expectedRecord
    }

    "update record when update finite product (old action)" in {
      val product = ProductWithDurationGen.next
      val action = FiniteProductActionGen.next
      val header = createCampaign(product, Some(action))

      val expectedRecord = FiniteProductDao.FiniteProductRecord(
        header.id,
        product.from.get,
        product.from.get.plus(product.duration.get),
        header.product,
        action
      )
      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.head shouldBe expectedRecord

      val newProduct = ProductWithDurationGen.next
      val patch = CampaignService.Patch(product = Some(newProduct))
      val updatedHeader = service.update(customerId, header.id, patch).get

      val newExpectedRecord = FiniteProductDao.FiniteProductRecord(
        updatedHeader.id,
        newProduct.from.get,
        newProduct.from.get.plus(newProduct.duration.get),
        newProduct,
        expectedRecord.action
      )

      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.head shouldBe newExpectedRecord
    }

    "update record when update finite product (new action)" in {
      val product = ProductWithDurationGen.next
      val action = FiniteProductActionGen.next
      val header = createCampaign(product, Some(action))

      val expectedRecord = FiniteProductDao.FiniteProductRecord(
        header.id,
        product.from.get,
        product.from.get.plus(product.duration.get),
        header.product,
        action
      )
      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.head shouldBe expectedRecord

      val newProduct = ProductWithDurationGen.next
      val newAction = FiniteProductActionGen.next
      val patch = CampaignService.Patch(product = Some(newProduct), action = Some(newAction))
      val updatedHeader = service.update(customerId, header.id, patch).get

      val newExpectedRecord = FiniteProductDao.FiniteProductRecord(
        updatedHeader.id,
        newProduct.from.get,
        newProduct.from.get.plus(newProduct.duration.get),
        newProduct,
        newAction
      )

      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.head shouldBe newExpectedRecord
    }

    "delete record when update on product" in {
      val product = ProductWithDurationGen.next
      val action = FiniteProductActionGen.next
      val header = createCampaign(product, Some(action))

      val expectedRecord = FiniteProductDao.FiniteProductRecord(
        header.id,
        product.from.get,
        product.from.get.plus(product.duration.get),
        header.product,
        action
      )
      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.head shouldBe expectedRecord

      val newProduct = ProductGen.next
      val patch = CampaignService.Patch(product = Some(newProduct))
      service.update(customerId, header.id, patch).get

      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.headOption shouldBe None
    }

    "insert record when update on finite product" in {
      val product = ProductGen.next
      val header = createCampaign(product, None)

      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.headOption shouldBe None

      val withDurationProduct = ProductWithDurationGen.next
      val action = FiniteProductActionGen.next
      val patch = CampaignService.Patch(product = Some(withDurationProduct), action = Some(action))
      service.update(customerId, header.id, patch).get

      val expectedRecord = FiniteProductDao.FiniteProductRecord(
        header.id,
        withDurationProduct.from.get,
        withDurationProduct.from.get.plus(withDurationProduct.duration.get),
        withDurationProduct,
        action
      )

      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.head shouldBe expectedRecord
    }

    "fail update if no action" in {
      val product = ProductGen.next
      val header = createCampaign(product, None)

      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.headOption shouldBe None

      val withDurationProduct = ProductWithDurationGen.next
      val patch = CampaignService.Patch(product = Some(withDurationProduct))
      intercept[IllegalArgumentException] {
        service.update(customerId, header.id, patch).get
      }

      finiteProductDao.get(FiniteProductDao.ForCampaign(header.id)).get.headOption shouldBe None
    }

  }

}
