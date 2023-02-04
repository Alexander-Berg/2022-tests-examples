package ru.auto.salesman.tasks.user

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{AdditionalInfo, Category, Offer}
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.user.GoodsDao.Filter.ActivatedSince
import ru.auto.salesman.model.offer.{AutoruOfferId, OfferIdentity}
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  ProductStatus,
  ProductStatuses,
  Slave
}
import ru.auto.salesman.model.user.Goods
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.service.EpochService
import ru.auto.salesman.service.user.GoodsService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

class SetCountersStartDateAfterOfferActivationTaskSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
  implicit val rc: RequestContext = AutomatedContext("unit-test")

  val goodsService = mock[GoodsService]
  val vosClient = mock[VosClient]
  val epochService = mock[EpochService]

  val task = new SetCountersStartDateAfterOfferActivationTask(
    goodsService,
    vosClient,
    epochService
  )

  private val epochMarker = "SetCountersStartDateAfterOfferActivation"

  private val getEpoch =
    (epochService.getOptional _)
      .expects(epochMarker)

  "SetCountersStartDateAfterOfferActivationTask" should {
    "clear offers counters: golden way" in {
      val epoch = timestamp("2020-03-01T00:00:00.000+03:00")

      getEpoch.returningT {
        Some(epoch)
      }

      val (offer1, activated1) =
        (AutoruOfferId("123-fff"), dateTime("2020-03-02T01:00:00.000+03:00"))
      val (offer2, activated2) =
        (AutoruOfferId("456-fff"), dateTime("2020-03-02T02:00:00.000+03:00"))

      val goods = Iterable(
        placement(offer1, ProductStatuses.Active, activated1),
        placement(offer2, ProductStatuses.Active, activated2)
      )

      (goodsService.get _)
        .expects(ActivatedSince(dateTime(epoch), Placement))
        .returningZ(goods)

      (vosClient.getOffer _)
        .expects(offer1, Slave)
        .returningZ {
          offerWithCountersStartDate(timestamp("2020-03-01T10:00:00.000+03:00"))
        }

      (vosClient.getOffer _)
        .expects(offer2, Slave)
        .returningZ {
          offerWithCountersStartDate(timestamp("2020-03-01T12:00:00.000+03:00"))
        }

      (vosClient
        .setCountersStartDate(_: OfferIdentity, _: Category, _: DateTime)(
          _: RequestContext
        ))
        .expects(offer1, Category.CARS, activated1, *)
        .returningT(())

      (vosClient
        .setCountersStartDate(_: OfferIdentity, _: Category, _: DateTime)(
          _: RequestContext
        ))
        .expects(offer2, Category.CARS, activated2, *)
        .returningT(())

      (epochService.set _)
        .expects(epochMarker, activated2.getMillis)
        .returningT(())

      task.execute().success.value shouldBe (())
    }

    "filter already updated counters start date" in {
      val epoch = timestamp("2020-03-01T00:00:00.000+03:00")

      getEpoch.returningT {
        Some(epoch)
      }

      val (offer1, activated1) =
        (AutoruOfferId("123-fff"), dateTime("2020-03-02T01:00:00.000+03:00"))
      val (offer2, activated2) =
        (AutoruOfferId("456-fff"), dateTime("2020-03-02T02:00:00.000+03:00"))

      val goods = Iterable(
        placement(offer1, ProductStatuses.Active, activated1),
        placement(offer2, ProductStatuses.Active, activated2)
      )

      (goodsService.get _)
        .expects(ActivatedSince(dateTime(epoch), Placement))
        .returningZ(goods)

      (vosClient.getOffer _)
        .expects(offer1, Slave)
        .returningZ {
          offerWithCountersStartDate(timestamp("2020-03-01T10:00:00.000+03:00"))
        }

      (vosClient.getOffer _)
        .expects(offer2, Slave)
        .returningZ {
          offerWithCountersStartDate(timestamp("2020-03-20T12:00:00.000+03:00"))
        }

      (vosClient
        .setCountersStartDate(_: OfferIdentity, _: Category, _: DateTime)(
          _: RequestContext
        ))
        .expects(offer1, Category.CARS, activated1, *)
        .returningT(())

      (vosClient
        .setCountersStartDate(_: OfferIdentity, _: Category, _: DateTime)(
          _: RequestContext
        ))
        .expects(offer2, Category.CARS, activated2, *)
        .never()

      (epochService.set _)
        .expects(epochMarker, activated2.getMillis)
        .returningT(())

      task.execute().success.value shouldBe (())
    }

    "fail on empty epoch" in {
      getEpoch.returningT {
        None
      }

      task.execute().failure.exception shouldBe a[NoSuchElementException]
    }

    "dont set epoch on processing error" in {
      val epoch = timestamp("2020-03-01T00:00:00.000+03:00")

      getEpoch.returningT {
        Some(epoch)
      }

      val (offer1, activated1) =
        (AutoruOfferId("123-fff"), dateTime("2020-03-02T01:00:00.000+03:00"))
      val (offer2, activated2) =
        (AutoruOfferId("456-fff"), dateTime("2020-03-02T02:00:00.000+03:00"))

      val goods = Iterable(
        placement(offer1, ProductStatuses.Active, activated1),
        placement(offer2, ProductStatuses.Active, activated2)
      )

      (goodsService.get _)
        .expects(ActivatedSince(dateTime(epoch), Placement))
        .returningZ(goods)

      (vosClient.getOffer _)
        .expects(offer1, Slave)
        .returningZ {
          offerWithCountersStartDate(timestamp("2020-03-01T10:00:00.000+03:00"))
        }

      (vosClient
        .setCountersStartDate(_: OfferIdentity, _: Category, _: DateTime)(
          _: RequestContext
        ))
        .expects(offer1, Category.CARS, activated1, *)
        .returningT(())

      (vosClient.getOffer _)
        .expects(offer2, Slave)
        .throwingZ {
          new IllegalArgumentException("Some VOS error")
        }

      (vosClient
        .setCountersStartDate(_: OfferIdentity, _: Category, _: DateTime)(
          _: RequestContext
        ))
        .expects(offer2, Category.CARS, activated2, *)
        .never()

      (epochService.set _)
        .expects(*, *)
        .never()

      task.execute().failure.exception shouldBe an[IllegalArgumentException]
    }
  }

  def placement(
      offerId: OfferIdentity,
      status: ProductStatus,
      activated: DateTime
  ): Goods =
    goodsGen(
      offerIdentity = Gen.const(offerId),
      goodsProduct = Gen.const(Placement),
      status = Gen.const(status),
      activated = Gen.const(activated)
    ).next

  def dateTime(dt: String): DateTime =
    DateTime.parse(dt)

  def dateTime(ts: Long): DateTime =
    new DateTime(ts)

  def timestamp(dt: String): Long =
    dateTime(dt).getMillis

  def timestamp(dt: DateTime): Long =
    dt.getMillis

  def offerWithCountersStartDate(timestamp: Long): Offer =
    offerGen(
      offerCategoryGen = Gen.const(
        Category.CARS
      ),
      additionalInfoGen = Gen.const(
        AdditionalInfo
          .newBuilder()
          .setCountersStartDate(timestamp)
          .build()
      )
    ).next

}
