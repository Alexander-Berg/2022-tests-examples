package ru.auto.salesman.tasks

import org.joda.time.DateTime
import org.scalatest.Ignore
import ru.auto.salesman.dao.GoodsDao.Record
import ru.auto.salesman.model._
import ru.auto.salesman.push.PushPathResolver
import ru.auto.salesman.push.impl.AsyncPushClientImpl
import ru.auto.salesman.service.EpochService
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.test.TestAkkaComponents._
import ru.auto.salesman.test.dao.gens._
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.model.Dsl
import ru.yandex.vertis.generators.ProducerProvider.asProducer

import scala.collection.JavaConverters._
import scala.util.Success

@Ignore
trait OfferBillingGenPushSpec extends DeprecatedMockitoBaseSpec {

  def pushPathResolver: PushPathResolver

  private def task(records: Iterable[Record]) = {
    val client =
      new AsyncPushClientImpl(pushPathResolver)

    new OfferBillingPushSharderTask(
      client,
      OfferBillingPushSharderTaskSpec.goodsDao(records),
      2,
      MockEpochService,
      Markers.OfferBillingPushSharderEpoch
    )(system.dispatcher)
  }

  private val MockEpochService = {
    val m = mock[EpochService]
    when(m.get(?))
      .thenReturn(Success(0L))

    when(m.set(?, ?))
      .thenReturn(Success(()))
    m
  }

  private val CampaignId = "test-id"
  private val CampaignName = "test-campaign"
  private val OrderId = 1111L
  private val ClientId = 1L
  private val ProductKey = "default"
  private val Customer = Dsl.customer(ClientId, false, 0L)
  private val ResourceRef = Dsl.partnerResourceRef("1438536")

  private val CustomerHeader =
    Dsl.customerHeader(Customer, List(ResourceRef).asJava)

  private val Order =
    Dsl.order(
      OrderId,
      Customer,
      "test",
      0L,
      0L,
      0L,
      0L,
      "test",
      ProductKey,
      null
    )

  private val CampaignSettings =
    Model.CampaignSettings
      .newBuilder()
      .setIsEnabled(true)
      .setVersion(1)
      .build()
  private val Cost = Dsl.costPerIndexing(100L)

  private def knownCampaign(offerId: PartnerOfferId, productId: ProductId) = {
    val id = Dsl.partnerOfferId(offerId.partner.get, offerId.value)
    val good = Dsl.custom(productId.toString, Cost, null, 0L)

    val product = Dsl.product(Set(good).asJava, "tag")

    val campaignHeader =
      Model.CampaignHeader
        .newBuilder()
        .setId(CampaignId)
        .setName(CampaignName)
        .setOrder(Order)
        .setOwner(CustomerHeader)
        .setSettings(CampaignSettings)
        .setProduct(product)
        .setVersion(1)
        .build()

    val now = DateTime.now()
    Dsl.knownCampaign(
      id,
      now.getMillis,
      Model.BindingSource.API,
      campaignHeader,
      true,
      null,
      now.plusDays(1).getMillis,
      now.getMillis,
      "hold",
      now.getMillis
    )
  }

  def toRecord(
      offerId: OfferId,
      offerHash: OfferHash,
      billing: Model.OfferBilling,
      gs: GoodStatus
  ): Record =
    GoodRecordGen.next.copy(
      offerId = offerId,
      offerHash = offerHash,
      status = gs,
      offerBilling = Some(billing.toByteArray)
    )

  "OfferBillingPushSharderGen" should {
    import GoodStatuses._

    "push" in {
      def enabled(offerCategory: OfferCategory) = {
        val id = 1002338765L
        val offerHash = "a7e82a"
        val offerId = AutoRuOfferId(id, offerCategory)
        Iterable(
          (id, offerHash, knownCampaign(offerId, ProductId.Placement), Active),
          (id, offerHash, knownCampaign(offerId, ProductId.Premium), Active),
          (
            id,
            offerHash,
            knownCampaign(offerId, ProductId.PremiumOffer),
            Active
          ),
          (id, offerHash, knownCampaign(offerId, ProductId.Color), Active)
        )
      }

      def different(offerCategory: OfferCategory) = {
        val id = 1002429251L
        val offerHash = "ac6074"
        val offerId = AutoRuOfferId(id, offerCategory)
        Iterable(
          (id, offerHash, knownCampaign(offerId, ProductId.Placement), Active),
          (id, offerHash, knownCampaign(offerId, ProductId.Premium), Inactive),
          (
            id,
            offerHash,
            knownCampaign(offerId, ProductId.PremiumOffer),
            Active
          ),
          (id, offerHash, knownCampaign(offerId, ProductId.Color), Inactive)
        )
      }

      def disabled(offerCategory: OfferCategory) = {
        val id = 1002944974L
        val offerHash = "2271"
        val offerId = AutoRuOfferId(id, offerCategory)
        Iterable(
          (
            id,
            offerHash,
            knownCampaign(offerId, ProductId.Placement),
            Inactive
          ),
          (id, offerHash, knownCampaign(offerId, ProductId.Premium), Inactive),
          (
            id,
            offerHash,
            knownCampaign(offerId, ProductId.PremiumOffer),
            Inactive
          ),
          (id, offerHash, knownCampaign(offerId, ProductId.Color), Inactive)
        )
      }

      val records = (enabled(OfferCategories.Cars)
        ++ different(OfferCategories.Cars)
        ++ disabled(OfferCategories.Cars)).map { case (oId, oh, b, av) =>
        toRecord(oId, oh, b, av)
      }
      task(records).run()

      val trucksRecords = (enabled(OfferCategories.Trailer)
        ++ different(OfferCategories.Trailer)
        ++ disabled(OfferCategories.Trailer)).map { case (oId, oh, b, av) =>
        toRecord(oId, oh, b, av)
      }
      task(trucksRecords).run()

      val motoRecords = (enabled(OfferCategories.Scooters)
        ++ different(OfferCategories.Scooters)
        ++ disabled(OfferCategories.Scooters)).map { case (oId, oh, b, av) =>
        toRecord(oId, oh, b, av)
      }
      task(motoRecords).run()
    }
  }

}
