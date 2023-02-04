package ru.auto.salesman.service.offer

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.dao.GoodsDao.Filter.AlreadyBilled
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.{
  FirstActivateDate,
  GoodStatuses,
  OfferCategories,
  OfferProductActiveDays,
  ProductId
}
import ru.auto.salesman.service.GoodsDaoProvider
import ru.auto.salesman.test.BaseSpec

class OfferInfoServiceSpec extends BaseSpec {

  private val goodsDaoProvider = mock[GoodsDaoProvider]
  private val vosClient = mock[VosClient]
  private val goodsDao = mock[GoodsDao]

  private val offerInfoService =
    new OfferInfoService(goodsDaoProvider, vosClient)

  "OfferInfoService" should {

    "return offer placement product duration in days" in {
      (goodsDaoProvider.chooseDao _)
        .expects(*)
        .returning(goodsDao)

      val offerId = 123456
      val category = OfferCategories.Cars
      val firstActivateDate =
        FirstActivateDate(DateTime.parse("2019-01-01T01:00:00+03:00"))
      val offerBillingDeadlineDate = DateTime.parse("2019-01-03T01:00:00+03:00")
      (goodsDao.get _)
        .expects(AlreadyBilled(offerId, category, ProductId.Placement))
        .returningT(
          List(
            getRecord(firstActivateDate, offerBillingDeadlineDate),
            getRecord(
              FirstActivateDate(firstActivateDate.plusDays(1)),
              offerBillingDeadlineDate
            )
          )
        )

      val result = offerInfoService.productActivityDuration(
        category,
        AutoruOfferId(offerId, "abcd"),
        ProductId.Placement
      )
      result.get shouldBe OfferProductActiveDays(
        Some(2 /*first placement*/ + 1 /*second placement*/ )
      )
    }

    "return None product duration if no goods found" in {
      (goodsDaoProvider.chooseDao _)
        .expects(*)
        .returning(goodsDao)

      val offerId = 123456
      val category = OfferCategories.Cars
      (goodsDao.get _)
        .expects(AlreadyBilled(offerId, category, ProductId.Placement))
        .returningT(Nil)

      val result = offerInfoService.productActivityDuration(
        category,
        AutoruOfferId(offerId, "abcd"),
        ProductId.Placement
      )
      result.get shouldBe OfferProductActiveDays(None)
    }

  }

  private def getRecord(
      firstActivateDate: FirstActivateDate,
      offerBillingDeadlineDate: DateTime
  ) =
    GoodsDao.Record(
      1,
      123456,
      "abcd",
      OfferCategories.Cars,
      Section.NEW,
      20101,
      ProductId.Placement,
      GoodStatuses.Active,
      DateTime.now(),
      "",
      None,
      firstActivateDate,
      None,
      Some(offerBillingDeadlineDate),
      None,
      None
    )

}
