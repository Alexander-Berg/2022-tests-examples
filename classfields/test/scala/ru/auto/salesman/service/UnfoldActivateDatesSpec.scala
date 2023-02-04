package ru.auto.salesman.service

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Category.CARS
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.dao.GoodsDao.Filter.AlreadyBilled
import ru.auto.salesman.model.OfferCategories.Cars
import ru.auto.salesman.model.ProductId.{Placement, Turbo}
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.{FirstActivateDate, ProductDuration, Slave}
import ru.auto.salesman.service.client.ClientService
import ru.auto.salesman.service.impl.GoodsDaoProviderImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.goodRecordGen
import ru.auto.salesman.test.model.gens.{ClientRecordGen, OfferModelGenerators}
import ru.auto.salesman.util.offer._

class UnfoldActivateDatesSpec extends BaseSpec with OfferModelGenerators {

  private val vosClient = mock[VosClient]
  private val clientService = mock[ClientService]
  private val getProductDuration = mock[GetProductDuration]
  private val goodsDao = mock[GoodsDao]
  private val categorizedGoodsDao = mock[GoodsDao]

  private val goodsDaoProvider =
    new GoodsDaoProviderImpl(goodsDao, categorizedGoodsDao)

  private val unfoldActivateDates =
    new UnfoldActivateDates(
      vosClient,
      clientService,
      getProductDuration,
      goodsDaoProvider
    )

  private val clientGen = ClientRecordGen
  private val offerId = AutoruOfferId("1092070638-01148318")
  private val nonHashedOfferId = 1092070638

  private val carsOfferGen =
    offerGen(offerIdGen = offerId, offerCategoryGen = CARS)

  "unfoldActivateDates()" should {

    "generate for placement all dates with duration = 1.day before 2019-10-01" in {
      val goodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-09-01T12:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-09-05T12:00:00+03:00")
      )
      forAll(clientGen, carsOfferGen, goodGen) { (client, offer, good) =>
        import client.clientId
        val offerId = offer.id
        (clientService.getByIdOrFail _)
          .expects(clientId, /* withDeleted = */ true)
          .returningZ(client)
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)
        def mockGetDuration(activateDate: DateTime): Unit =
          (getProductDuration.apply _)
            .expects(client, offer, Placement, activateDate)
            .returningZ(ProductDuration.days(1))
        mockGetDuration(DateTime.parse("2019-09-01T12:00:00+03:00"))
        mockGetDuration(DateTime.parse("2019-09-02T12:00:00+03:00"))
        mockGetDuration(DateTime.parse("2019-09-03T12:00:00+03:00"))
        mockGetDuration(DateTime.parse("2019-09-04T12:00:00+03:00"))
        (goodsDao.get _)
          .expects(AlreadyBilled(nonHashedOfferId, Cars, Placement))
          .returningT(List(good))
        val result =
          unfoldActivateDates
            .unfold(clientId, offerId, Placement, latestActivateDate = None)
            .success
            .value
        result should contain theSameElementsAs List(
          DateTime.parse("2019-09-01T12:00:00+03:00"),
          DateTime.parse("2019-09-02T12:00:00+03:00"),
          DateTime.parse("2019-09-03T12:00:00+03:00"),
          DateTime.parse("2019-09-04T12:00:00+03:00")
        )
      }
    }

    "generate for placement all dates with duration = 1.day since 2010-10-03 and before 2019-10-01" in {
      val goodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-09-01T12:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-09-05T12:00:00+03:00")
      )
      forAll(clientGen, carsOfferGen, goodGen) { (client, offer, good) =>
        import client.clientId
        val offerId = offer.id
        (clientService.getByIdOrFail _)
          .expects(clientId, /* withDeleted = */ true)
          .returningZ(client)
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)
        def mockGetDuration(activateDate: DateTime): Unit =
          (getProductDuration.apply _)
            .expects(client, offer, Placement, activateDate)
            .returningZ(ProductDuration.days(1))
        mockGetDuration(DateTime.parse("2019-09-03T12:00:00+03:00"))
        mockGetDuration(DateTime.parse("2019-09-04T12:00:00+03:00"))
        (goodsDao.get _)
          .expects(AlreadyBilled(nonHashedOfferId, Cars, Placement))
          .returningT(List(good))
        val result =
          unfoldActivateDates
            .unfold(
              clientId,
              offerId,
              Placement,
              latestActivateDate = Some(DateTime.parse("2019-09-03T12:00:00+03:00"))
            )
            .success
            .value
        result should contain theSameElementsAs List(
          DateTime.parse("2019-09-03T12:00:00+03:00"),
          DateTime.parse("2019-09-04T12:00:00+03:00")
        )
      }
    }

    "generate for placement all dates with duration = 1.day before 2019-10-01 and 60.days after 2019-10-01" in {
      val goodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-09-29T14:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-11-30T14:00:00+03:00")
      )
      forAll(clientGen, carsOfferGen, goodGen) { (client, offer, good) =>
        import client.clientId
        val offerId = offer.id
        (clientService.getByIdOrFail _)
          .expects(clientId, /* withDeleted = */ true)
          .returningZ(client)
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)
        def mockGetDuration(
            activateDate: DateTime,
            duration: ProductDuration
        ): Unit =
          (getProductDuration.apply _)
            .expects(client, offer, Placement, activateDate)
            .returningZ(duration)
        mockGetDuration(
          DateTime.parse("2019-09-29T14:00:00+03:00"),
          ProductDuration.days(1)
        )
        mockGetDuration(
          DateTime.parse("2019-09-30T14:00:00+03:00"),
          ProductDuration.days(1)
        )
        mockGetDuration(
          DateTime.parse("2019-10-01T14:00:00+03:00"),
          ProductDuration.days(60)
        )
        (goodsDao.get _)
          .expects(AlreadyBilled(nonHashedOfferId, Cars, Placement))
          .returningT(List(good))
        val result =
          unfoldActivateDates
            .unfold(clientId, offerId, Placement, latestActivateDate = None)
            .success
            .value
        result should contain theSameElementsAs List(
          DateTime.parse("2019-09-29T14:00:00+03:00"),
          DateTime.parse("2019-09-30T14:00:00+03:00"),
          DateTime.parse("2019-10-01T14:00:00+03:00")
        )
      }
    }

    "don't generate for placement firstActivateDate from overlapping activations" in {
      val firstGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-10-03T15:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-12-02T15:00:00+03:00")
      )
      val secondGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-10-06T18:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2020-01-31T15:00:00+03:00")
      )
      val goodsGen = for {
        firstGood <- firstGoodGen
        secondGood <- secondGoodGen
      } yield List(firstGood, secondGood)
      forAll(clientGen, carsOfferGen, goodsGen) { (client, offer, goods) =>
        import client.clientId
        val offerId = offer.id
        (clientService.getByIdOrFail _)
          .expects(clientId, /* withDeleted = */ true)
          .returningZ(client)
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)
        (getProductDuration.apply _)
          .expects(client, offer, Placement, *)
          .returningZ(ProductDuration.days(60))
          .anyNumberOfTimes()
        (goodsDao.get _)
          .expects(AlreadyBilled(nonHashedOfferId, Cars, Placement))
          .returningT(goods)
        val result =
          unfoldActivateDates
            .unfold(clientId, offerId, Placement, latestActivateDate = None)
            .success
            .value
        result should contain theSameElementsAs List(
          DateTime.parse("2019-10-03T15:00:00+03:00"),
          DateTime.parse("2019-12-02T15:00:00+03:00")
        )
      }
    }

    "generate for both placements when they don't overlap" in {
      val firstGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-09-24T15:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-09-25T15:00:00+03:00")
      )
      val secondGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-09-27T16:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-09-28T16:00:00+03:00")
      )
      val goodsGen = for {
        firstGood <- firstGoodGen
        secondGood <- secondGoodGen
      } yield List(firstGood, secondGood)
      forAll(clientGen, carsOfferGen, goodsGen) { (client, offer, goods) =>
        import client.clientId
        val offerId = offer.id
        (clientService.getByIdOrFail _)
          .expects(clientId, /* withDeleted = */ true)
          .returningZ(client)
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)
        (getProductDuration.apply _)
          .expects(client, offer, Placement, *)
          .returningZ(ProductDuration.days(1))
          .anyNumberOfTimes()
        (goodsDao.get _)
          .expects(AlreadyBilled(nonHashedOfferId, Cars, Placement))
          .returningT(goods)
        val result =
          unfoldActivateDates
            .unfold(clientId, offerId, Placement, latestActivateDate = None)
            .success
            .value
        result should contain theSameElementsAs List(
          DateTime.parse("2019-09-24T15:00:00+03:00"),
          DateTime.parse("2019-09-27T16:00:00+03:00")
        )
      }
    }

    "generate for placements with offerBillingDeadline after latest activate date" in {
      val firstGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-09-24T15:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-09-25T15:00:00+03:00")
      )
      val secondGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-09-27T16:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-09-28T16:00:00+03:00")
      )
      val goodsGen = for {
        firstGood <- firstGoodGen
        secondGood <- secondGoodGen
      } yield List(firstGood, secondGood)
      forAll(clientGen, carsOfferGen, goodsGen) { (client, offer, goods) =>
        import client.clientId
        val offerId = offer.id
        (clientService.getByIdOrFail _)
          .expects(clientId, /* withDeleted = */ true)
          .returningZ(client)
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)
        (getProductDuration.apply _)
          .expects(client, offer, Placement, *)
          .returningZ(ProductDuration.days(1))
          .anyNumberOfTimes()
        (goodsDao.get _)
          .expects(AlreadyBilled(nonHashedOfferId, Cars, Placement))
          .returningT(goods)
        val result =
          unfoldActivateDates
            .unfold(
              clientId,
              offerId,
              Placement,
              latestActivateDate = Some(DateTime.parse("2019-09-27T15:00:00+03:00"))
            )
            .success
            .value
        result should contain theSameElementsAs List(
          DateTime.parse("2019-09-27T16:00:00+03:00")
        )
      }
    }

    "generate for both placements when they don't overlap and are in wrong order" in {
      val firstGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-09-27T16:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-09-28T16:00:00+03:00")
      )
      val secondGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-09-24T15:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-09-25T15:00:00+03:00")
      )
      val goodsGen = for {
        firstGood <- firstGoodGen
        secondGood <- secondGoodGen
      } yield List(firstGood, secondGood)
      forAll(clientGen, carsOfferGen, goodsGen) { (client, offer, goods) =>
        import client.clientId
        val offerId = offer.id
        (clientService.getByIdOrFail _)
          .expects(clientId, /* withDeleted = */ true)
          .returningZ(client)
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)
        (getProductDuration.apply _)
          .expects(client, offer, Placement, *)
          .returningZ(ProductDuration.days(1))
          .anyNumberOfTimes()
        (goodsDao.get _)
          .expects(AlreadyBilled(nonHashedOfferId, Cars, Placement))
          .returningT(goods)
        val result =
          unfoldActivateDates
            .unfold(clientId, offerId, Placement, latestActivateDate = None)
            .success
            .value
        result should contain theSameElementsAs List(
          DateTime.parse("2019-09-24T15:00:00+03:00"),
          DateTime.parse("2019-09-27T16:00:00+03:00")
        )
      }
    }

    "generate for 100k overlapped placements without StackOverflowError" in {
      val firstGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-10-01T15:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-11-30T15:00:00+03:00")
      )
      val nextGoodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-10-05T16:00:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-11-30T15:00:00+03:00")
      )
      val goodsGen = for {
        firstGood <- firstGoodGen
        nextGood <- nextGoodGen
      } yield firstGood :: List.fill(100000)(nextGood)
      forAll(clientGen, carsOfferGen, goodsGen) { (client, offer, goods) =>
        import client.clientId
        val offerId = offer.id
        (clientService.getByIdOrFail _)
          .expects(clientId, /* withDeleted = */ true)
          .returningZ(client)
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)
        (getProductDuration.apply _)
          .expects(client, offer, Placement, *)
          .returningZ(ProductDuration.days(60))
          .anyNumberOfTimes()
        (goodsDao.get _)
          .expects(AlreadyBilled(nonHashedOfferId, Cars, Placement))
          .returningT(goods)
        val result =
          unfoldActivateDates
            .unfold(clientId, offerId, Placement, latestActivateDate = None)
            .success
            .value
        result should contain theSameElementsAs List(
          DateTime.parse("2019-10-01T15:00:00+03:00")
        )
      }
    }

    "generate for turbo all dates with duration = 7.days" in {
      val goodGen = goodRecordGen(
        firstActivateDateGen =
          FirstActivateDate(DateTime.parse("2019-10-04T13:30:00+03:00")),
        offerBillingDeadlineGen = DateTime.parse("2019-10-11T13:30:00+03:00")
      )
      forAll(clientGen, carsOfferGen, goodGen) { (client, offer, good) =>
        import client.clientId
        val offerId = offer.id
        (clientService.getByIdOrFail _)
          .expects(clientId, /* withDeleted = */ true)
          .returningZ(client)
        (vosClient.getOffer _).expects(offerId, Slave).returningZ(offer)
        def mockGetDuration(activateDate: DateTime): Unit =
          (getProductDuration.apply _)
            .expects(client, offer, Turbo, activateDate)
            .returningZ(ProductDuration.days(7))
        mockGetDuration(DateTime.parse("2019-10-04T13:30:00+03:00"))
        (goodsDao.get _)
          .expects(AlreadyBilled(nonHashedOfferId, Cars, Turbo))
          .returningT(List(good))
        val result =
          unfoldActivateDates
            .unfold(clientId, offerId, Turbo, latestActivateDate = None)
            .success
            .value
        result should contain theSameElementsAs List(
          DateTime.parse("2019-10-04T13:30:00+03:00")
        )
      }
    }
  }
}
